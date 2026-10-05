package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.CashFlowBucket;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashFlowGranularity;
import br.gravita.core.domain.finance.CashFlowProjection;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GetCashFlowQuery;
import br.gravita.core.ports.outbound.finance.NegativeBalanceProjectionAlert;
import br.gravita.core.ports.outbound.finance.NotifyNegativeBalanceProjectionPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.GetCashFlowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCashFlowServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private NotifyNegativeBalanceProjectionPort notifyPort;

	private GetCashFlowService service;

	@BeforeEach
	void setUp() {
		service = new GetCashFlowService(receivableRepositoryPort, payableRepositoryPort, settlementRepositoryPort,
				notifyPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static GetCashFlowQuery daily(final LocalDate from, final LocalDate to) {
		return new GetCashFlowQuery(CashFlowGranularity.DAILY, null, null, null, null, from, to, null);
	}

	private static Receivable receivable(final String amount, final LocalDate dueDate, final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal(amount), dueDate, null, status, null, null);
	}

	private static Payable payable(final String amount, final LocalDate dueDate, final List<CostCenterShare> split) {
		return Payable.builder()
				.id(PayableId.of(UUID.randomUUID()))
				.supplierId(null)
				.origin(PayableOrigin.MANUAL)
				.amount(new BigDecimal(amount))
				.dueDate(dueDate)
				.costCenterSplit(split)
				.status(PayableStatus.OPEN)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.build();
	}

	private static Settlement receiptOf(final Receivable receivable, final String amount, final String interest, final Instant at) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount),
				new BigDecimal(interest), null, null, null, at);
	}

	private static Settlement paymentOf(final Payable payable, final String amount, final Instant at) {
		return Settlement.ofPayable(SettlementId.of(UUID.randomUUID()), payable.getId(), new BigDecimal(amount), null,
				null, null, null, SettlementMethod.MANUAL, at);
	}

	private static CashFlowBucket bucketOn(final CashFlowProjection projection, final LocalDate date) {
		return projection.getBuckets().stream().filter(bucket -> bucket.periodStart().equals(date)).findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("Combines the realized settlements of both sides with the open titles by due date")
	void combinesTheRealizedSettlementsOfBothSidesWithTheOpenTitlesByDueDate() {
		final LocalDate from = TODAY.minusDays(1);
		final LocalDate to = TODAY.plusDays(5);
		final Receivable paidReceivable = receivable("100.00", from, ReceivableStatus.SETTLED);
		final Payable paidPayable = payable("40.00", from, List.of());
		when(settlementRepositoryPort.findRealizedBetween(any(), any(), eq(CashFlowFilter.NONE)))
				.thenReturn(List.of(receiptOf(paidReceivable, "100.00", "2.00", NOW.minusSeconds(86_400)),
						paymentOf(paidPayable, "40.00", NOW.minusSeconds(86_400))));
		final Receivable openReceivable = receivable("200.00", TODAY.plusDays(2), ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findOutstandingDueUntil(to, CashFlowFilter.NONE))
				.thenReturn(List.of(openReceivable));
		when(payableRepositoryPort.findOutstandingDueUntil(to, CashFlowFilter.NONE))
				.thenReturn(List.of(payable("30.00", TODAY.plusDays(4), List.of())));

		final CashFlowProjection projection = service.execute(daily(from, to));

		final CashFlowBucket yesterday = bucketOn(projection, from);
		assertThat(yesterday.realizedInflow()).isEqualByComparingTo("102.00");
		assertThat(yesterday.realizedOutflow()).isEqualByComparingTo("40.00");
		assertThat(yesterday.balance()).isEqualByComparingTo("62.00");
		assertThat(bucketOn(projection, TODAY.plusDays(2)).projectedInflow()).isEqualByComparingTo("200.00");
		assertThat(bucketOn(projection, TODAY.plusDays(4)).projectedOutflow()).isEqualByComparingTo("30.00");
		assertThat(projection.getClosingBalance()).isEqualByComparingTo("232.00");
		verify(notifyPort, never()).notify(any());
	}

	@Test
	@DisplayName("Projects an open receivable for what is still owed on it")
	void anOpenReceivableIsProjectedForWhatIsStillOwedOnIt() {
		final Receivable partlyPaid = receivable("100.00", TODAY.plusDays(1), ReceivableStatus.PARTIALLY_SETTLED);
		when(receivableRepositoryPort.findOutstandingDueUntil(any(), any())).thenReturn(List.of(partlyPaid));
		when(settlementRepositoryPort.findByReceivableIds(List.of(partlyPaid.getId())))
				.thenReturn(List.of(receiptOf(partlyPaid, "50.00", "3.00", NOW.minusSeconds(3_600))));

		final CashFlowProjection projection = service.execute(daily(TODAY, TODAY.plusDays(2)));

		assertThat(bucketOn(projection, TODAY.plusDays(1)).projectedInflow()).isEqualByComparingTo("50.00");
	}

	@Test
	@DisplayName("Projects an overdue title on today instead of its past due date")
	void anOverdueTitleIsProjectedOnTodayInsteadOfItsPastDueDate() {
		when(receivableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(receivable("70.00", TODAY.minusDays(10), ReceivableStatus.OPEN)));
		when(payableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(payable("20.00", TODAY.minusDays(3), List.of())));

		final CashFlowProjection projection = service.execute(daily(TODAY.minusDays(30), TODAY.plusDays(5)));

		final CashFlowBucket today = bucketOn(projection, TODAY);
		assertThat(today.projectedInflow()).isEqualByComparingTo("70.00");
		assertThat(today.projectedOutflow()).isEqualByComparingTo("20.00");
		assertThat(bucketOn(projection, TODAY.minusDays(10)).projectedInflow()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Passes all four filters to the ports and to the alert")
	void passesAllFourFiltersToThePortsAndTheAlert() {
		final UUID company = UUID.randomUUID();
		final UUID branch = UUID.randomUUID();
		final UUID bankAccount = UUID.randomUUID();
		final UUID costCenter = UUID.randomUUID();
		final CashFlowFilter filter = new CashFlowFilter(company, branch, bankAccount, costCenter);
		final LocalDate to = TODAY.plusDays(3);
		final Payable inCostCenter = payable("10.00", TODAY.plusDays(1), List.of(new CostCenterShare(costCenter,
				new BigDecimal("100"))));
		when(payableRepositoryPort.findOutstandingDueUntil(to, filter)).thenReturn(List.of(inCostCenter));

		service.execute(new GetCashFlowQuery(CashFlowGranularity.DAILY, company, branch, bankAccount, costCenter,
				TODAY, to, null));

		verify(receivableRepositoryPort).findOutstandingDueUntil(to, filter);
		verify(payableRepositoryPort).findOutstandingDueUntil(to, filter);
		verify(settlementRepositoryPort).findRealizedBetween(any(), any(), eq(filter));
		final ArgumentCaptor<NegativeBalanceProjectionAlert> alert = ArgumentCaptor
				.forClass(NegativeBalanceProjectionAlert.class);
		verify(notifyPort).notify(alert.capture());
		assertThat(alert.getValue().filter()).isEqualTo(filter);
	}

	@Test
	@DisplayName("Counts only the cost center's share of each payable, open or paid")
	void costCenterCountsOnlyItsShareOfEachPayableOpenOrPaid() {
		final UUID costCenter = UUID.randomUUID();
		final UUID other = UUID.randomUUID();
		final CashFlowFilter filter = new CashFlowFilter(null, null, null, costCenter);
		final Payable split = payable("200.00", TODAY.plusDays(1), List.of(new CostCenterShare(costCenter,
				new BigDecimal("25")), new CostCenterShare(other, new BigDecimal("75"))));
		final Payable paid = payable("100.00", TODAY.minusDays(1), List.of(new CostCenterShare(costCenter,
				new BigDecimal("40")), new CostCenterShare(other, new BigDecimal("60"))));
		when(payableRepositoryPort.findOutstandingDueUntil(any(), eq(filter))).thenReturn(List.of(split));
		when(settlementRepositoryPort.findRealizedBetween(any(), any(), eq(filter)))
				.thenReturn(List.of(paymentOf(paid, "100.00", NOW.minusSeconds(86_400))));
		when(payableRepositoryPort.findByIds(List.of(paid.getId()))).thenReturn(List.of(paid));

		final CashFlowProjection projection = service.execute(new GetCashFlowQuery(CashFlowGranularity.DAILY, null, null,
				null, costCenter, TODAY.minusDays(1), TODAY.plusDays(2), null));

		assertThat(bucketOn(projection, TODAY.plusDays(1)).projectedOutflow()).isEqualByComparingTo("50.00");
		assertThat(bucketOn(projection, TODAY.minusDays(1)).realizedOutflow()).isEqualByComparingTo("40.00");
	}

	@Test
	@DisplayName("Raises an alert with the first negative period and the lowest balance when the projected balance goes negative")
	void anAlertIsRaisedWhenTheProjectedBalanceGoesNegativeAndCarriesTheFirstNegativePeriodAndTheLowestBalance() {
		when(payableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(payable("500.00", TODAY.plusDays(2), List.of())));
		when(receivableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(receivable("200.00", TODAY.plusDays(4), ReceivableStatus.OPEN)));

		service.execute(new GetCashFlowQuery(CashFlowGranularity.DAILY, null, null, null, null, TODAY,
				TODAY.plusDays(5), new BigDecimal("100.00")));

		final ArgumentCaptor<NegativeBalanceProjectionAlert> alert = ArgumentCaptor
				.forClass(NegativeBalanceProjectionAlert.class);
		verify(notifyPort).notify(alert.capture());
		assertThat(alert.getValue().firstNegativePeriodStart()).isEqualTo(TODAY.plusDays(2));
		assertThat(alert.getValue().lowestBalance()).isEqualByComparingTo("-400.00");
		assertThat(alert.getValue().granularity()).isEqualTo(CashFlowGranularity.DAILY);
	}

	@Test
	@DisplayName("Raises no alert while the balance stays non-negative")
	void noAlertWhileTheBalanceStaysNonNegative() {
		when(payableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(payable("100.00", TODAY.plusDays(2), List.of())));

		service.execute(new GetCashFlowQuery(CashFlowGranularity.DAILY, null, null, null, null, TODAY,
				TODAY.plusDays(5), new BigDecimal("100.00")));

		verify(notifyPort, never()).notify(any());
	}

	@Test
	@DisplayName("Raises no alert for a negative balance that is already in the past")
	void negativeBalanceThatIsAlreadyInThePastDoesNotAlert() {
		final Receivable paid = receivable("10.00", TODAY.minusDays(9), ReceivableStatus.SETTLED);
		final Payable spent = payable("500.00", TODAY.minusDays(9), List.of());
		when(settlementRepositoryPort.findRealizedBetween(any(), any(), any()))
				.thenReturn(List.of(paymentOf(spent, "500.00", NOW.minusSeconds(9 * 86_400)),
						receiptOf(paid, "10.00", "0", NOW.minusSeconds(9 * 86_400))));
		when(receivableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(receivable("1000.00", TODAY, ReceivableStatus.OPEN)));

		service.execute(daily(TODAY.minusDays(10), TODAY.plusDays(2)));

		verify(notifyPort, never()).notify(any());
	}

	@Test
	@DisplayName("Does not fail the cash flow view when the notification fails")
	void failingNotificationDoesNotFailTheView() {
		when(payableRepositoryPort.findOutstandingDueUntil(any(), any()))
				.thenReturn(List.of(payable("500.00", TODAY.plusDays(2), List.of())));
		doThrow(new IllegalStateException("queue down")).when(notifyPort).notify(any());

		final CashFlowProjection projection = service.execute(daily(TODAY, TODAY.plusDays(3)));

		assertThat(projection.getClosingBalance()).isEqualByComparingTo("-500.00");
	}

	@Test
	@DisplayName("Defaults the range to thirty days back and ninety days ahead")
	void theRangeDefaultsToThirtyDaysBackAndNinetyDaysAhead() {
		final CashFlowProjection projection = service.execute(new GetCashFlowQuery(CashFlowGranularity.MONTHLY, null, null,
				null, null, null, null, null));

		assertThat(projection.getFrom()).isEqualTo(TODAY.minusDays(30));
		assertThat(projection.getTo()).isEqualTo(TODAY.plusDays(90));
		assertThat(projection.getGranularity()).isEqualTo(CashFlowGranularity.MONTHLY);
		verify(receivableRepositoryPort).findOutstandingDueUntil(TODAY.plusDays(90), CashFlowFilter.NONE);
	}

	@Test
	@DisplayName("Rejects a range that is inverted or too long")
	void rejectsAnInvertedOrExcessiveRange() {
		assertThatThrownBy(() -> service.execute(daily(TODAY, TODAY.minusDays(1))))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> service.execute(daily(TODAY, TODAY.plusYears(11))))
				.isInstanceOf(BusinessRuleException.class);
	}
}
