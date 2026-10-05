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

	public ClosePosSessionService(final PosSessionRepositoryPort posSessionRepositoryPort,
			final NfceRepositoryPort nfceRepositoryPort, final CashMovementRepositoryPort cashMovementRepositoryPort,
			final CashClosingReportRepositoryPort cashClosingReportRepositoryPort,
			final PrintNonFiscalReceiptPort printNonFiscalReceiptPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.cashMovementRepositoryPort = cashMovementRepositoryPort;
		this.cashClosingReportRepositoryPort = cashClosingReportRepositoryPort;
		this.printNonFiscalReceiptPort = printNonFiscalReceiptPort;
	}

	@Override
	public CashClosingReport execute(final ClosePosSessionCommand command) {
		final PosSessionId sessionId = PosSessionId.of(command.sessionId());
		final PosSession session = posSessionRepositoryPort.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("PosSession not found: " + command.sessionId()));

		final Instant closedAt = Instant.now();
		final PosSession closedSession = session.close(closedAt);

		final List<NfceSale> issuedSales = nfceRepositoryPort.findBySessionId(sessionId).stream()
				.filter(sale -> ISSUED_STATUSES.contains(sale.getStatus()))
				.toList();

		final Map<PaymentMethodType, BigDecimal> expectedAmounts = new EnumMap<>(PaymentMethodType.class);
		for (final NfceSale sale : issuedSales) {
			for (final Payment payment : sale.getPayments()) {
				expectedAmounts.merge(payment.method(), payment.amount(), BigDecimal::add);
			}
		}

		final List<CashMovement> movements = cashMovementRepositoryPort.findBySessionId(sessionId);
		final BigDecimal totalSangria = sumByType(movements, CashMovementType.SANGRIA);
		final BigDecimal totalSuprimento = sumByType(movements, CashMovementType.SUPRIMENTO);

		final CashClosingReport report = CashClosingReport.builder()
				.id(CashClosingReportId.of(UUID.randomUUID()))
				.sessionId(sessionId)
				.registerId(session.getRegisterId())
				.operatorId(session.getOperatorId())
				.openingAmount(session.getOpeningChangeAmount())
				.expectedAmountsByPaymentMethod(expectedAmounts)
				.countedAmountsByPaymentMethod(command.closingCountedAmounts())
				.totalSangriaAmount(totalSangria)
				.totalSuprimentoAmount(totalSuprimento)
				.saleCount(issuedSales.size())
				.openedAt(session.getOpenedAt())
				.closedAt(closedAt)
				.build();

		posSessionRepositoryPort.save(closedSession);
		final CashClosingReport savedReport = cashClosingReportRepositoryPort.save(report);
		printNonFiscalReceiptPort.print(savedReport);

		return savedReport;
	}

	private static BigDecimal sumByType(final List<CashMovement> movements, final CashMovementType type) {
		return movements.stream()
				.filter(movement -> movement.getType() == type)
				.map(CashMovement::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
