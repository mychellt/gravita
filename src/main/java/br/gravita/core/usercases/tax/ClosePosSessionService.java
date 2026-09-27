package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.CashClosingReportId;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.inbound.tax.ClosePosSessionCommand;
import br.gravita.core.ports.inbound.tax.ClosePosSessionUseCase;
import br.gravita.core.ports.outbound.persistence.tax.CashClosingReportRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.CashMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.PrintNonFiscalReceiptPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@UseCase
public class ClosePosSessionService implements ClosePosSessionUseCase {

	private static final Set<NfceSaleStatus> ISSUED_STATUSES = Set.of(NfceSaleStatus.AUTHORIZED,
			NfceSaleStatus.PENDING_SYNC);

	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final NfceRepositoryPort nfceRepositoryPort;
	private final CashMovementRepositoryPort cashMovementRepositoryPort;
	private final CashClosingReportRepositoryPort cashClosingReportRepositoryPort;
	private final PrintNonFiscalReceiptPort printNonFiscalReceiptPort;

	public ClosePosSessionService(PosSessionRepositoryPort posSessionRepositoryPort,
			NfceRepositoryPort nfceRepositoryPort, CashMovementRepositoryPort cashMovementRepositoryPort,
			CashClosingReportRepositoryPort cashClosingReportRepositoryPort,
			PrintNonFiscalReceiptPort printNonFiscalReceiptPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.cashMovementRepositoryPort = cashMovementRepositoryPort;
		this.cashClosingReportRepositoryPort = cashClosingReportRepositoryPort;
		this.printNonFiscalReceiptPort = printNonFiscalReceiptPort;
	}

	@Override
	public CashClosingReport execute(ClosePosSessionCommand command) {
		PosSessionId sessionId = PosSessionId.of(command.sessionId());
		PosSession session = posSessionRepositoryPort.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("PosSession not found: " + command.sessionId()));

		Instant closedAt = Instant.now();
		PosSession closedSession = session.close(closedAt);

		List<NfceSale> issuedSales = nfceRepositoryPort.findBySessionId(sessionId).stream()
				.filter(sale -> ISSUED_STATUSES.contains(sale.getStatus()))
				.toList();

		Map<PaymentMethodType, BigDecimal> expectedAmounts = new EnumMap<>(PaymentMethodType.class);
		for (NfceSale sale : issuedSales) {
			for (Payment payment : sale.getPayments()) {
				expectedAmounts.merge(payment.method(), payment.amount(), BigDecimal::add);
			}
		}

		List<CashMovement> movements = cashMovementRepositoryPort.findBySessionId(sessionId);
		BigDecimal totalSangria = sumByType(movements, CashMovementType.SANGRIA);
		BigDecimal totalSuprimento = sumByType(movements, CashMovementType.SUPRIMENTO);

		CashClosingReport report = CashClosingReport.close(CashClosingReportId.of(UUID.randomUUID()), sessionId,
				session.getRegisterId(), session.getOperatorId(), session.getOpeningChangeAmount(), expectedAmounts,
				command.closingCountedAmounts(), totalSangria, totalSuprimento, issuedSales.size(),
				session.getOpenedAt(), closedAt);

		posSessionRepositoryPort.save(closedSession);
		CashClosingReport savedReport = cashClosingReportRepositoryPort.save(report);
		printNonFiscalReceiptPort.print(savedReport);

		return savedReport;
	}

	private static BigDecimal sumByType(List<CashMovement> movements, CashMovementType type) {
		return movements.stream()
				.filter(movement -> movement.getType() == type)
				.map(CashMovement::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
