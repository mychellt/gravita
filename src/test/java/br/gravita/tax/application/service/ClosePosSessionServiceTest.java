package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.ports.inbound.tax.ClosePosSessionCommand;
import br.gravita.core.ports.outbound.persistence.tax.CashClosingReportRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.CashMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.PrintNonFiscalReceiptPort;
import br.gravita.core.usercases.tax.ClosePosSessionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClosePosSessionServiceTest {

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private NfceRepositoryPort nfceRepositoryPort;

	@Mock
	private CashMovementRepositoryPort cashMovementRepositoryPort;

	@Mock
	private CashClosingReportRepositoryPort cashClosingReportRepositoryPort;

	@Mock
	private PrintNonFiscalReceiptPort printNonFiscalReceiptPort;

	private ClosePosSessionService service;

	@BeforeEach
	void setUp() {
		service = new ClosePosSessionService(posSessionRepositoryPort, nfceRepositoryPort, cashMovementRepositoryPort,
				cashClosingReportRepositoryPort, printNonFiscalReceiptPort);
	}

	private PosSession openSession(UUID sessionId) {
		return PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.OPEN, Instant.now(), null);
	}

	private NfceSale issuedSale(PosSessionId sessionId, PaymentMethodType method, BigDecimal amount) {
		return NfceSale.of(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(new SaleItem(UUID.randomUUID(), BigDecimal.ONE, amount, BigDecimal.ZERO)), BigDecimal.ZERO,
				List.of(new Payment(method, amount)), BigDecimal.ZERO, null, NfceSaleStatus.AUTHORIZED, Instant.now(),
				"1", 1L, "access-key", "protocol", false);
	}

	private NfceSale draftSale(PosSessionId sessionId) {
		return NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId,
				List.of(new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO)),
				BigDecimal.ZERO, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null,
				Instant.now());
	}

	private CashMovement movement(PosSessionId sessionId, CashMovementType type, BigDecimal amount) {
		return CashMovement.of(CashMovementId.of(UUID.randomUUID()), sessionId, type, amount, "justification",
				Instant.now());
	}

	@Test
	void ac1_reconciliationTotalsAreBrokenDownByPaymentMethod() {
		UUID sessionId = UUID.randomUUID();
		PosSessionId posSessionId = PosSessionId.of(sessionId);
		when(posSessionRepositoryPort.findById(posSessionId)).thenReturn(Optional.of(openSession(sessionId)));
		when(nfceRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of(
				issuedSale(posSessionId, PaymentMethodType.CASH, new BigDecimal("40.00")),
				issuedSale(posSessionId, PaymentMethodType.PIX, new BigDecimal("25.00"))));
		when(cashMovementRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashClosingReportRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CashClosingReport report = service.execute(new ClosePosSessionCommand(sessionId, Map.of()));

		assertThat(report.getExpectedAmountsByPaymentMethod())
				.isEqualTo(Map.of(PaymentMethodType.CASH, new BigDecimal("40.00"), PaymentMethodType.PIX,
						new BigDecimal("25.00")));
	}

	@Test
	void ac1_draftSalesAreExcludedFromTheReconciliation() {
		UUID sessionId = UUID.randomUUID();
		PosSessionId posSessionId = PosSessionId.of(sessionId);
		when(posSessionRepositoryPort.findById(posSessionId)).thenReturn(Optional.of(openSession(sessionId)));
		when(nfceRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of(draftSale(posSessionId)));
		when(cashMovementRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashClosingReportRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CashClosingReport report = service.execute(new ClosePosSessionCommand(sessionId, Map.of()));

		assertThat(report.getSaleCount()).isZero();
		assertThat(report.getExpectedAmountsByPaymentMethod()).isEmpty();
	}

	@Test
	void ac2_theReportIncludesOpeningAmountCashMovementsAndSaleCount() {
		UUID sessionId = UUID.randomUUID();
		PosSessionId posSessionId = PosSessionId.of(sessionId);
		when(posSessionRepositoryPort.findById(posSessionId)).thenReturn(Optional.of(openSession(sessionId)));
		when(nfceRepositoryPort.findBySessionId(posSessionId)).thenReturn(
				List.of(issuedSale(posSessionId, PaymentMethodType.CASH, new BigDecimal("40.00"))));
		when(cashMovementRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of(
				movement(posSessionId, CashMovementType.SANGRIA, new BigDecimal("15.00")),
				movement(posSessionId, CashMovementType.SUPRIMENTO, new BigDecimal("5.00"))));
		when(cashClosingReportRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CashClosingReport report = service.execute(new ClosePosSessionCommand(sessionId, Map.of()));

		assertThat(report.getOpeningAmount()).isEqualByComparingTo("100.00");
		assertThat(report.getTotalSangriaAmount()).isEqualByComparingTo("15.00");
		assertThat(report.getTotalSuprimentoAmount()).isEqualByComparingTo("5.00");
		assertThat(report.getSaleCount()).isEqualTo(1);
	}

	@Test
	void ac3_theReportIsPrintedAfterClosing() {
		UUID sessionId = UUID.randomUUID();
		PosSessionId posSessionId = PosSessionId.of(sessionId);
		when(posSessionRepositoryPort.findById(posSessionId)).thenReturn(Optional.of(openSession(sessionId)));
		when(nfceRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashMovementRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashClosingReportRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CashClosingReport report = service.execute(new ClosePosSessionCommand(sessionId, Map.of()));

		ArgumentCaptor<CashClosingReport> captor = ArgumentCaptor.forClass(CashClosingReport.class);
		verify(printNonFiscalReceiptPort).print(captor.capture());
		assertThat(captor.getValue()).isEqualTo(report);
	}

	@Test
	void ac4_theSessionTransitionsToClosed() {
		UUID sessionId = UUID.randomUUID();
		PosSessionId posSessionId = PosSessionId.of(sessionId);
		when(posSessionRepositoryPort.findById(posSessionId)).thenReturn(Optional.of(openSession(sessionId)));
		when(nfceRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashMovementRepositoryPort.findBySessionId(posSessionId)).thenReturn(List.of());
		when(cashClosingReportRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ClosePosSessionCommand(sessionId, Map.of()));

		ArgumentCaptor<PosSession> captor = ArgumentCaptor.forClass(PosSession.class);
		verify(posSessionRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(PosSessionStatus.CLOSED);
	}

	@Test
	void ac4_closingAnAlreadyClosedSessionIsRejected() {
		UUID sessionId = UUID.randomUUID();
		PosSession closedSession = PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.CLOSED, Instant.now(),
				Instant.now());
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.of(closedSession));

		assertThatThrownBy(() -> service.execute(new ClosePosSessionCommand(sessionId, Map.of())))
				.isInstanceOf(BusinessRuleException.class);

		verify(cashClosingReportRepositoryPort, never()).save(any());
		verify(printNonFiscalReceiptPort, never()).print(any());
	}

	@Test
	void closingAnUnknownSessionIsRejected() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ClosePosSessionCommand(sessionId, Map.of())))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
