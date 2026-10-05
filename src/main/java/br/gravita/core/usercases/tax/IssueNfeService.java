package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeAccessKeyGenerator;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransportInfo;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.ItemCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.RecipientCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.TransportCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.inbound.tax.TaxOverrideCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * UC-M2-01. Unlike NFC-e's {@link IssueNfceService} (which submits to SEFAZ
 * inline), an NFe never goes further than {@code QUEUED} here - transmission
 * is a separate, asynchronous consumer ({@code TransmitNfeUseCase}, GRA-103)
 * this use case only enqueues for.
 */
@UseCase
public class IssueNfeService implements IssueNfeUseCase {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final PriceTableRepositoryPort priceTableRepositoryPort;
	private final CfopRegistryPort cfopRegistryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;
	private final TransmissionQueuePort transmissionQueuePort;

	public IssueNfeService(final NfeRepositoryPort nfeRepositoryPort, final CompanyRepositoryPort companyRepositoryPort,
			final PriceTableRepositoryPort priceTableRepositoryPort, final CfopRegistryPort cfopRegistryPort,
			final CalculateTaxUseCase calculateTaxUseCase, final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase,
			final TransmissionQueuePort transmissionQueuePort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.priceTableRepositoryPort = priceTableRepositoryPort;
		this.cfopRegistryPort = cfopRegistryPort;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.allocateDocumentNumberUseCase = allocateDocumentNumberUseCase;
		this.transmissionQueuePort = transmissionQueuePort;
	}

	@Override
	public NfeDocument execute(final IssueNfeCommand command) {
		final Company company = companyRepositoryPort.findById(CompanyId.of(command.issuerCompanyId()))
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + command.issuerCompanyId()));

		// AC3: recipient CPF/CNPJ check-digit + IE-taxpayer validation.
		final NfeRecipient recipient = buildRecipient(command.recipient());

		// AC2: CFOP always comes from the registry, keyed by operation type.
		final Cfop cfop = cfopRegistryPort.resolve(command.naturezaOperacao());

		// AC5: discount vs. the linked price table's max-discount rule, unless justified.
		final PriceTable priceTable = resolvePriceTable(command.priceTableId());
		checkDiscounts(priceTable, command.items(), command.discountOverrideJustification());

		// AC4: a manual tax override is only accepted with a justification.
		requireJustifiedOverrides(command.taxOverrides());

		// AC4: tax totals always come from the shared engine, never recomputed here.
		final TaxCalculationResult taxResult = calculateTaxUseCase.execute(buildTaxCommand(command, company, recipient));
		final List<NfeItem> items = buildItems(command.items(), taxResult.items());

		// AC1/AC6: originSalesOrderId and referencedAccessKey are preserved/enforced by the aggregate itself.
		final NfeDocument draft = NfeDocument.draft()
				.id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(company.getId())
				.originSalesOrderId(command.originSalesOrderId())
				.naturezaOperacao(command.naturezaOperacao())
				.cfop(cfop)
				.recipient(recipient)
				.items(items)
				.freight(command.freight())
				.insurance(command.insurance())
				.otherExpenses(command.otherExpenses())
				.transport(buildTransport(command.transport()))
				.referencedAccessKey(command.referencedAccessKey())
				.additionalInfo(command.additionalInfo())
				.taxTotals(taxResult.totals())
				.createdAt(Instant.now())
				.build();

		final DocumentNumber documentNumber = allocateDocumentNumberUseCase
				.execute(new AllocateDocumentNumberCommand(company.getId(), FiscalDocumentType.NFE));
		final String accessKey = NfeAccessKeyGenerator.generate(company.getState(), company.getCnpj().number(),
				documentNumber.series(), documentNumber.number());

		// AC7: on success, the document is QUEUED and a TransmissionQueueEntry exists for it.
		final NfeDocument queued = draft.queue(documentNumber.series(), documentNumber.number(), accessKey);
		final NfeDocument saved = nfeRepositoryPort.save(queued);
		transmissionQueuePort.enqueue(saved.getId());

		return saved;
	}

	private NfeRecipient buildRecipient(final RecipientCommand recipient) {
		return NfeRecipient.of(PersonRef.of(recipient.personId()), recipient.document(), recipient.personType(),
				recipient.name(), recipient.stateRegistration(), recipient.state());
	}

	private NfeTransportInfo buildTransport(final TransportCommand transport) {
		if (transport == null) {
			return null;
		}
		return new NfeTransportInfo(transport.modality(), transport.carrier(), transport.volume(),
				transport.grossWeight(), transport.netWeight(), transport.rntrc());
	}

	private PriceTable resolvePriceTable(final UUID priceTableId) {
		if (priceTableId == null) {
			return null;
		}
		return priceTableRepositoryPort.findById(PriceTableId.of(priceTableId))
				.orElseThrow(() -> new ResourceNotFoundException("PriceTable not found: " + priceTableId));
	}

	/**
	 * AC5: mirrors {@code RegisterNfceSaleService#checkDiscount} - a money
	 * discount is converted to a percentage of the item's own subtotal before
	 * being checked against the table, since {@link PriceTable#evaluateDiscount}
	 * works in percentage terms. A non-blank {@code overrideJustification}
	 * bypasses the limit entirely, per the AC's "unless override is explicitly
	 * justified".
	 */
	private void checkDiscounts(final PriceTable priceTable, final List<ItemCommand> items, final String overrideJustification) {
		if (priceTable == null || (overrideJustification != null && !overrideJustification.isBlank())) {
			return;
		}
		for (final ItemCommand item : items) {
			final BigDecimal discount = item.discount();
			final BigDecimal subtotal = item.unitPrice().multiply(item.quantity());
			if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0
					|| subtotal.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			final BigDecimal discountPercent = discount.divide(subtotal, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
			priceTable.evaluateDiscount(discountPercent);
		}
	}

	private void requireJustifiedOverrides(final List<TaxOverrideCommand> overrides) {
		for (final TaxOverrideCommand override : overrides) {
			if (override.justification() == null || override.justification().isBlank()) {
				throw new BusinessRuleException(
						"Manual tax override for item " + override.itemIndex() + " requires a justification");
			}
		}
	}

	private CalculateTaxCommand buildTaxCommand(final IssueNfeCommand command, final Company company, final NfeRecipient recipient) {
		final List<TaxItemCommand> items = command.items().stream()
				.map(item -> new TaxItemCommand(item.productId().toString(), item.quantity(), item.unitPrice()))
				.toList();
		// masterdata.TaxRegime and tax.TaxRegime are separate enums with the same
		// values (one per module's package boundary); convert by name at the seam.
		final TaxRegime taxRegime = TaxRegime.valueOf(company.getTaxRegime().name());
		return new CalculateTaxCommand(items, company.getState(), recipient.state(), taxRegime,
				command.naturezaOperacao().name(), command.taxOverrides());
	}

	private List<NfeItem> buildItems(final List<ItemCommand> inputs, final List<ItemTaxBreakdown> breakdowns) {
		final List<NfeItem> items = new ArrayList<>();
		for (int index = 0; index < inputs.size(); index++) {
			final ItemCommand input = inputs.get(index);
			final int itemIndex = index;
			final ItemTaxBreakdown breakdown = breakdowns.stream().filter(b -> b.itemIndex() == itemIndex).findFirst()
					.orElseThrow(() -> new BusinessRuleException("Missing tax breakdown for item index " + itemIndex));
			items.add(new NfeItem(input.productId(), input.description(), input.quantity(), input.unitPrice(),
					input.discount(), breakdown));
		}
		return items;
	}
}
