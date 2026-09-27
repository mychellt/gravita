package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
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

/**
 * UC-M2-10. Confirms the three-way conference (ordered vs. physically
 * received vs. what the NF itself states), then - synchronously, in the same
 * request, per AC2 - registers the stock entry into {@code inventory},
 * notifies {@code finance} of the payable, and runs the shared tax engine for
 * the ICMS/PIS/COFINS credit under the company's tax regime in effect now.
 * Independent from M6's {@code ConfirmPurchaseReceiptUseCase} (GRA-63): same
 * shape of side effects, different (NFe-driven) trigger, own ports.
 */
@UseCase
public class ConfirmInboundNfeReceiptService implements ConfirmInboundNfeReceiptUseCase {

	private static final String OPERATION_TYPE = "ENTRADA_COMPRA";
	private static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final NotifyStockEntryPort notifyStockEntryPort;
	private final NotifyPayableGeneratedPort notifyPayableGeneratedPort;

	public ConfirmInboundNfeReceiptService(InboundNfeRepositoryPort inboundNfeRepositoryPort,
			CompanyRepositoryPort companyRepositoryPort, CalculateTaxUseCase calculateTaxUseCase,
			NotifyStockEntryPort notifyStockEntryPort, NotifyPayableGeneratedPort notifyPayableGeneratedPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.notifyStockEntryPort = notifyStockEntryPort;
		this.notifyPayableGeneratedPort = notifyPayableGeneratedPort;
	}

	@Override
	public InboundNfe execute(ConfirmInboundNfeReceiptCommand command) {
		InboundNfeId id = InboundNfeId.of(command.inboundNfeId());
		InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(id)
				.orElseThrow(() -> new InboundNfeNotFoundException(command.inboundNfeId()));

		List<InboundNfeConferenceItem> conferenceResult = toConferenceItems(command.conferenceResult());

		// AC1: records the three-way comparison and guards "already confirmed" before
		// any side effect runs, so a retried request never double-triggers them.
		InboundNfe confirmed = inboundNfe.confirm(conferenceResult);

		Company company = companyRepositoryPort.findById(confirmed.getCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + confirmed.getCompanyId().value()));

		// AC2: one stock entry per conferenced line, registered now - not queued for a
		// later batch job.
		for (int i = 0; i < conferenceResult.size(); i++) {
			InboundNfeConferenceItem conferenceItem = conferenceResult.get(i);
			InboundNfeItem nfItem = confirmed.getItems().get(i);
			notifyStockEntryPort.notifyEntry(new NotifyStockEntryCommand(conferenceItem.itemRef(),
					conferenceItem.receivedQty(), nfItem.unitValue(), confirmed.getId().value()));
		}

		// AC3: installments generated from the NF's own data, not re-entered manually.
		notifyPayableGeneratedPort.notifyGenerated(new NotifyPayableGeneratedCommand(confirmed.getId().value(),
				confirmed.getSupplierDocument().number(), toInstallments(confirmed)));

		// AC4: ICMS/PIS/COFINS credit always comes from the shared tax engine, per the
		// company's tax regime in effect now - never recomputed by hand here.
		calculateTaxUseCase.execute(buildTaxCommand(confirmed, conferenceResult, company));

		return inboundNfeRepositoryPort.save(confirmed);
	}

	private List<InboundNfeConferenceItem> toConferenceItems(
			List<ConfirmInboundNfeReceiptCommand.ConferenceItem> conferenceResult) {
		return conferenceResult.stream()
				.map(item -> new InboundNfeConferenceItem(item.itemRef(), item.orderedQty(), item.receivedQty()))
				.toList();
	}

	private List<Installment> toInstallments(InboundNfe confirmed) {
		// InboundNfe doesn't carry duplicata/payment-terms data yet - UC-M2-08/09
		// don't parse it - so, until they do, the NF's total value is billed as a
		// single installment, net 30 from issuance. Mirrors purchasing's own
		// RegisterStockEntryAdapter falling back to a placeholder where upstream
		// data it needs doesn't exist yet.
		LocalDate dueDate = confirmed.getIssuedAt().atZone(ZoneOffset.UTC).toLocalDate()
				.plusDays(DEFAULT_PAYMENT_TERM_DAYS);
		return List.of(new Installment(confirmed.getTotals().totalValue(), dueDate));
	}

	private CalculateTaxCommand buildTaxCommand(InboundNfe confirmed, List<InboundNfeConferenceItem> conferenceResult,
			Company company) {
		List<TaxItemCommand> items = new ArrayList<>();
		for (int i = 0; i < conferenceResult.size(); i++) {
			InboundNfeItem nfItem = confirmed.getItems().get(i);
			items.add(new TaxItemCommand(conferenceResult.get(i).itemRef().toString(), nfItem.quantity(),
					nfItem.unitValue()));
		}
		// masterdata.TaxRegime and tax.TaxRegime are separate enums with the same
		// values (one per module's package boundary); convert by name at the seam,
		// same as IssueNfceService does.
		TaxRegime taxRegime = TaxRegime.valueOf(company.getTaxRegime().name());
		// InboundNfe doesn't carry the supplier's own UF yet (only its CNPJ), so both
		// origin and destination resolve to the receiving company's state until it does.
		return new CalculateTaxCommand(items, company.getState(), company.getState(), taxRegime, OPERATION_TYPE,
				List.of());
	}
}
