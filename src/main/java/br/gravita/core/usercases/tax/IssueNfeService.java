package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeAccessKeyGenerator;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransport;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.inbound.tax.NfeItemInput;
import br.gravita.core.ports.inbound.tax.NfeItemTaxOverrideInput;
import br.gravita.core.ports.inbound.tax.NfeRecipientInput;
import br.gravita.core.ports.inbound.tax.NfeTransportInput;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.inbound.tax.TaxOverrideCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UC-M2-01. CFOP (AC2) and tax totals (AC4) are always resolved here, never
 * hardcoded/recomputed - the former from each item's product registry
 * ({@code ProductDomain#getDefaultCfopByOperation}, keyed by
 * {@code naturezaOperacao}), the latter from the shared
 * {@link CalculateTaxUseCase}, which already rejects an unjustified
 * override (see {@code TaxOverrideInput}). {@code referencedAccessKey}'s
 * requirement for returns/complementary notes (AC6) is enforced by
 * {@link NfeDocument} itself, so it can't be bypassed by any caller.
 */
@UseCase
public class IssueNfeService implements IssueNfeUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final PriceTableRepositoryPort priceTableRepositoryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;
	private final TransmissionQueuePort transmissionQueuePort;

	@Autowired
	public IssueNfeService(NfeRepositoryPort nfeRepositoryPort, CompanyRepositoryPort companyRepositoryPort,
			ProductRepositoryPort productRepositoryPort, PriceTableRepositoryPort priceTableRepositoryPort,
			CalculateTaxUseCase calculateTaxUseCase, AllocateDocumentNumberUseCase allocateDocumentNumberUseCase,
			TransmissionQueuePort transmissionQueuePort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
		this.priceTableRepositoryPort = priceTableRepositoryPort;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.allocateDocumentNumberUseCase = allocateDocumentNumberUseCase;
		this.transmissionQueuePort = transmissionQueuePort;
	}

	@Override
	public NfeDocument execute(IssueNfeCommand command) {
		Company company = companyRepositoryPort.findById(command.issuerCompanyId())
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + command.issuerCompanyId().value()));
		NfeRecipient recipient = resolveRecipient(command.recipient()); // AC3
		PriceTable priceTable = resolvePriceTable(command.priceTableId());

		List<ProductDomain> products = resolveProducts(command.items());
		List<String> cfops = resolveCfops(products, command.items(), command.naturezaOperacao()); // AC2
		checkDiscounts(priceTable, command.items()); // AC5

		TaxCalculationResult taxResult = calculateTaxUseCase.execute(buildTaxCommand(command, company, recipient)); // AC4

		List<NfeItem> items = buildItems(command.items(), cfops, taxResult);
		NfeTransport transport = toTransport(command.transport());

		NfeDocument draft = NfeDocument.draft(NfeDocumentId.of(UUID.randomUUID()), company.getId(),
				command.originSalesOrderId(), command.naturezaOperacao(), recipient, items, // AC1
				command.freight(), command.insurance(), command.otherExpenses(), transport,
				command.referencedAccessKey(), command.additionalInfo(), taxResult.totals()); // AC6

		DocumentNumber documentNumber = allocateDocumentNumberUseCase
				.execute(new AllocateDocumentNumberCommand(company.getId(), FiscalDocumentType.NFE));
		String accessKey = NfeAccessKeyGenerator.generate(company.getState(), company.getCnpj().number(),
				documentNumber.series(), documentNumber.number());
		NfeDocument queued = draft.queue(documentNumber.series(), documentNumber.number(), accessKey);

		NfeDocument saved = nfeRepositoryPort.save(queued);
		transmissionQueuePort.enqueue(saved.getId()); // AC7
		return saved;
	}

	private NfeRecipient resolveRecipient(NfeRecipientInput input) {
		Document document = input.documentType() == PersonType.COMPANY ? Document.cnpj(input.document())
				: Document.cpf(input.document());
		return new NfeRecipient(input.customerId(), document, input.name(), input.ieIndicator(), input.ie(),
				input.state());
	}

	private PriceTable resolvePriceTable(UUID priceTableId) {
		if (priceTableId == null) {
			return null;
		}
		return priceTableRepositoryPort.findById(PriceTableId.of(priceTableId))
				.orElseThrow(() -> new ResourceNotFoundException("Price table not found: " + priceTableId));
	}

	private List<ProductDomain> resolveProducts(List<NfeItemInput> itemInputs) {
		List<ProductDomain> products = new ArrayList<>(itemInputs.size());
		for (NfeItemInput itemInput : itemInputs) {
			ProductDomain product = productRepositoryPort.get(itemInput.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemInput.productId()));
			products.add(product);
		}
		return products;
	}

	private List<String> resolveCfops(List<ProductDomain> products, List<NfeItemInput> itemInputs,
			String naturezaOperacao) {
		List<String> cfops = new ArrayList<>(products.size());
		for (int i = 0; i < products.size(); i++) {
			Map<String, String> registry = products.get(i).getDefaultCfopByOperation();
			String cfop = registry == null ? null : registry.get(naturezaOperacao);
			if (cfop == null || cfop.isBlank()) {
				throw new BusinessRuleException("No CFOP registered for product " + itemInputs.get(i).productId()
						+ " and operation " + naturezaOperacao);
			}
			cfops.add(cfop);
		}
		return cfops;
	}

	private void checkDiscounts(PriceTable priceTable, List<NfeItemInput> itemInputs) {
		if (priceTable == null) {
			return;
		}
		for (NfeItemInput itemInput : itemInputs) {
			if (itemInput.discountPercent().signum() == 0) {
				continue;
			}
			try {
				priceTable.evaluateDiscount(itemInput.discountPercent());
			} catch (br.gravita.core.domain.shared.BusinessRuleException exceededMaxDiscount) {
				if (itemInput.discountOverrideJustification() == null || itemInput.discountOverrideJustification().isBlank()) {
					throw exceededMaxDiscount;
				}
			}
		}
	}

	private CalculateTaxCommand buildTaxCommand(IssueNfeCommand command, Company company, NfeRecipient recipient) {
		List<TaxItemCommand> taxItems = command.items().stream()
				.map(item -> new TaxItemCommand(item.productId().toString(), item.quantity(), item.unitPrice()))
				.toList();
		List<TaxOverrideCommand> overrides = new ArrayList<>();
		List<NfeItemInput> itemInputs = command.items();
		for (int i = 0; i < itemInputs.size(); i++) {
			for (NfeItemTaxOverrideInput override : itemInputs.get(i).taxOverrides()) {
				overrides.add(new TaxOverrideCommand(i, override.tax(), override.value(), override.justification()));
			}
		}
		// masterdata.TaxRegime and tax.TaxRegime are separate enums with the same
		// values (one per module's package boundary); convert by name at the seam.
		br.gravita.core.domain.tax.TaxRegime taxRegime = br.gravita.core.domain.tax.TaxRegime
				.valueOf(company.getTaxRegime().name());
		return new CalculateTaxCommand(taxItems, company.getState(), recipient.state(), taxRegime,
				command.naturezaOperacao(), overrides);
	}

	private List<NfeItem> buildItems(List<NfeItemInput> itemInputs, List<String> cfops,
			TaxCalculationResult taxResult) {
		List<ItemTaxBreakdown> breakdowns = taxResult.items();
		List<NfeItem> items = new ArrayList<>(itemInputs.size());
		for (int i = 0; i < itemInputs.size(); i++) {
			NfeItemInput itemInput = itemInputs.get(i);
			items.add(new NfeItem(itemInput.productId(), itemInput.quantity(), itemInput.unitPrice(),
					itemInput.discountPercent(), cfops.get(i), breakdowns.get(i)));
		}
		return items;
	}

	private NfeTransport toTransport(NfeTransportInput input) {
		if (input == null) {
			return null;
		}
		return new NfeTransport(input.modality(), input.carrier(), input.volume(), input.grossWeight(),
				input.netWeight(), input.rntrc());
	}
}
