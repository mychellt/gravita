package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeResolution;
import br.gravita.core.domain.tax.InboundNfeResolution.ItemResolution;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
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
import java.util.stream.Collectors;

@UseCase
public class ConfirmInboundNfeReceiptService implements ConfirmInboundNfeReceiptUseCase {

	private static final String OPERATION_TYPE = "ENTRADA_COMPRA";
	private static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final NotifyStockEntryPort notifyStockEntryPort;
	private final NotifyPayableGeneratedPort notifyPayableGeneratedPort;
	private final InboundNfeResolver inboundNfeResolver;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	public ConfirmInboundNfeReceiptService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final CompanyRepositoryPort companyRepositoryPort, final CalculateTaxUseCase calculateTaxUseCase,
			final NotifyStockEntryPort notifyStockEntryPort, final NotifyPayableGeneratedPort notifyPayableGeneratedPort,
			final InboundNfeResolver inboundNfeResolver, final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.notifyStockEntryPort = notifyStockEntryPort;
		this.notifyPayableGeneratedPort = notifyPayableGeneratedPort;
		this.inboundNfeResolver = inboundNfeResolver;
		this.linkRepositoryPort = linkRepositoryPort;
	}

	@Override
	public InboundNfe execute(final ConfirmInboundNfeReceiptCommand command) {
		final InboundNfeId id = InboundNfeId.of(command.inboundNfeId());
		final InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(id)
				.orElseThrow(() -> new InboundNfeNotFoundException(command.inboundNfeId()));

		final List<InboundNfeConferenceItem> conferenceResult = toConferenceItems(command.conferenceResult());

		final InboundNfe confirmed = inboundNfe.confirm(conferenceResult);

		final InboundNfeResolution resolution = inboundNfeResolver.resolve(confirmed);
		requireResolved(resolution, conferenceResult);

		final Company company = companyRepositoryPort.findById(confirmed.getCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + confirmed.getCompanyId().value()));

		for (int i = 0; i < conferenceResult.size(); i++) {
			final InboundNfeConferenceItem conferenceItem = conferenceResult.get(i);
			final InboundNfeItem nfItem = confirmed.getItems().get(i);
			notifyStockEntryPort.notifyEntry(new NotifyStockEntryCommand(conferenceItem.itemRef(),
					conferenceItem.receivedQty(), nfItem.unitValue(), confirmed.getId().value()));
		}

		notifyPayableGeneratedPort.notifyGenerated(new NotifyPayableGeneratedCommand(confirmed.getId().value(),
				confirmed.getSupplierDocument().number(), toInstallments(confirmed)));

		if (company.getState() != null) {
			calculateTaxUseCase.execute(buildTaxCommand(confirmed, conferenceResult, company));
		}

		recordLastPurchase(resolution, confirmed);

		return inboundNfeRepositoryPort.save(confirmed);
	}

	private void requireResolved(final InboundNfeResolution resolution, final List<InboundNfeConferenceItem> conferenceResult) {
		if (!resolution.isSupplierResolved()) {
			throw new BusinessRuleException(
					"No registered supplier matches the CNPJ of this NF-e; register the supplier before confirming the receipt");
		}
		final List<ItemResolution> unresolved = resolution.unresolvedItems();
		if (!unresolved.isEmpty()) {
			throw new BusinessRuleException("Link a product to every NF-e item before confirming the receipt; unresolved: "
					+ describe(unresolved));
		}
		for (final ItemResolution itemResolution : resolution.items()) {
			final SupplierProductCatalogLink link = itemResolution.link().orElseThrow();
			if (!link.getProductId().equals(conferenceResult.get(itemResolution.index()).itemRef())) {
				throw new BusinessRuleException("Item " + (itemResolution.index() + 1) + " ("
						+ itemResolution.item().supplierProductCode() + ") must be received as the product linked to it");
			}
		}
	}

	private String describe(final List<ItemResolution> unresolved) {
		return unresolved.stream()
				.map(item -> "item " + (item.index() + 1) + " (" + item.item().supplierProductCode() + ")")
				.collect(Collectors.joining(", "));
	}

	/** The link keeps the last price paid, not an average; the imported item snapshot itself is never touched. */
	private void recordLastPurchase(final InboundNfeResolution resolution, final InboundNfe confirmed) {
		for (final ItemResolution itemResolution : resolution.items()) {
			linkRepositoryPort.save(itemResolution.link().orElseThrow()
					.withLastPurchase(itemResolution.item().unitValue(), confirmed.getIssuedAt()));
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
