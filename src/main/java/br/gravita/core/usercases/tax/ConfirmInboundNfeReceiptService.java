package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort.NotifyPayableGeneratedCommand;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort.NotifyPayableGeneratedCommand.Installment;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort.NotifyStockEntryCommand;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@UseCase
public class ConfirmInboundNfeReceiptService implements ConfirmInboundNfeReceiptUseCase {

	private static final String OPERATION_TYPE = "ENTRADA_COMPRA";
	private static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;
	private final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final NotifyStockEntryPort notifyStockEntryPort;
	private final NotifyPayableGeneratedPort notifyPayableGeneratedPort;

	public ConfirmInboundNfeReceiptService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final CompanyRepositoryPort companyRepositoryPort, final SupplierRepositoryPort supplierRepositoryPort,
			final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort,
			final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase,
			final CalculateTaxUseCase calculateTaxUseCase, final NotifyStockEntryPort notifyStockEntryPort,
			final NotifyPayableGeneratedPort notifyPayableGeneratedPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.linkRepositoryPort = linkRepositoryPort;
		this.upsertSupplierProductCatalogLinkUseCase = upsertSupplierProductCatalogLinkUseCase;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.notifyStockEntryPort = notifyStockEntryPort;
		this.notifyPayableGeneratedPort = notifyPayableGeneratedPort;
	}

	@Override
	public InboundNfe execute(final ConfirmInboundNfeReceiptCommand command) {
		final InboundNfeId id = InboundNfeId.of(command.inboundNfeId());
		final InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(id)
				.orElseThrow(() -> new InboundNfeNotFoundException(command.inboundNfeId()));

		final List<InboundNfeConferenceItem> conferenceResult = toConferenceItems(command.conferenceResult());

		final InboundNfe confirmed = inboundNfe.confirm(conferenceResult);

		final Supplier supplier = requireResolvedSupplier(confirmed);
		requireAllItemsResolved(confirmed, supplier, conferenceResult);

		final Company company = companyRepositoryPort.findById(confirmed.getCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + confirmed.getCompanyId().value()));

		for (int i = 0; i < conferenceResult.size(); i++) {
			final InboundNfeConferenceItem conferenceItem = conferenceResult.get(i);
			final InboundNfeItem nfItem = confirmed.getItems().get(i);
			notifyStockEntryPort.notifyEntry(new NotifyStockEntryCommand(conferenceItem.itemRef(),
					conferenceItem.receivedQty(), nfItem.unitValue(), confirmed.getId().value()));
			upsertSupplierProductCatalogLinkUseCase.execute(new UpsertSupplierProductCatalogLinkCommand(
					confirmed.getCompanyId().value(), supplier.getId().value(), nfItem.supplierProductCode(),
					conferenceItem.itemRef(), nfItem.unitValue(), confirmed.getIssuedAt()));
		}

		notifyPayableGeneratedPort.notifyGenerated(new NotifyPayableGeneratedCommand(confirmed.getId().value(),
				confirmed.getSupplierDocument().number(), toInstallments(confirmed)));

		calculateTaxUseCase.execute(buildTaxCommand(confirmed, conferenceResult, company));

		return inboundNfeRepositoryPort.save(confirmed);
	}

	/** BR1/AC2: a supplier must be registered for this NFe's CNPJ before its receipt can be confirmed. */
	private Supplier requireResolvedSupplier(final InboundNfe inboundNfe) {
		return supplierRepositoryPort.findByDocument(inboundNfe.getSupplierDocument())
				.orElseThrow(() -> new br.gravita.core.domain.shared.BusinessRuleException(
						"No supplier is registered for CNPJ " + inboundNfe.getSupplierDocument().formatted()
								+ "; register the supplier before confirming this receipt"));
	}

	/**
	 * BR2/BR3/AC3: every item must already carry a {@code SupplierProductCatalogLink} for (supplier, item code) -
	 * resolved via UC-M2-15/UC-M2-16 before confirmation is attempted. Also guards against confirming a different
	 * product than the one the link points to, which would otherwise corrupt BR7's price history.
	 */
	private void requireAllItemsResolved(final InboundNfe inboundNfe, final Supplier supplier,
			final List<InboundNfeConferenceItem> conferenceResult) {
		final List<InboundNfeItem> items = inboundNfe.getItems();
		for (int i = 0; i < items.size(); i++) {
			final InboundNfeItem item = items.get(i);
			final SupplierProductCatalogLink link = linkRepositoryPort
					.findByCompanySupplierAndCode(inboundNfe.getCompanyId(), supplier.getId(), item.supplierProductCode())
					.orElseThrow(() -> new br.gravita.core.domain.shared.BusinessRuleException("Item "
							+ item.supplierProductCode() + " (" + item.description()
							+ ") is not linked to a catalog product; resolve it before confirming the receipt"));
			if (i < conferenceResult.size() && !link.getProductId().equals(conferenceResult.get(i).itemRef())) {
				throw new br.gravita.core.domain.shared.BusinessRuleException("Item " + item.supplierProductCode()
						+ " was confirmed with a different product than its catalog link; re-check the conference");
			}
		}
	}

	private List<InboundNfeConferenceItem> toConferenceItems(
			final List<ConfirmInboundNfeReceiptCommand.ConferenceItem> conferenceResult) {
		return conferenceResult.stream()
				.map(item -> new InboundNfeConferenceItem(item.itemRef(), item.orderedQty(), item.receivedQty()))
				.toList();
	}

	private List<Installment> toInstallments(final InboundNfe confirmed) {
		final LocalDate dueDate = confirmed.getIssuedAt().atZone(ZoneOffset.UTC).toLocalDate()
				.plusDays(DEFAULT_PAYMENT_TERM_DAYS);
		return List.of(new Installment(confirmed.getTotals().totalValue(), dueDate));
	}

	private CalculateTaxCommand buildTaxCommand(final InboundNfe confirmed, final List<InboundNfeConferenceItem> conferenceResult,
			final Company company) {
		final List<TaxItemCommand> items = new ArrayList<>();
		for (int i = 0; i < conferenceResult.size(); i++) {
			final InboundNfeItem nfItem = confirmed.getItems().get(i);
			items.add(new TaxItemCommand(conferenceResult.get(i).itemRef().toString(), nfItem.quantity(),
					nfItem.unitValue()));
		}
		final TaxRegime taxRegime = TaxRegime.valueOf(company.getTaxRegime().name());
		return new CalculateTaxCommand(items, company.getState(), company.getState(), taxRegime, OPERATION_TYPE,
				List.of());
	}
}
