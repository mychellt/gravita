package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.AgingBucket;
import br.gravita.core.domain.finance.AgingRange;
import br.gravita.core.domain.finance.AgingReport;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.ports.inbound.finance.GetAgingListQuery;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.GetAgingListService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAgingListServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	private GetAgingListService service;

	@BeforeEach
	void setUp() {
		service = new GetAgingListService(receivableRepositoryPort, settlementRepositoryPort,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static Receivable receivable(final String amount, final LocalDate dueDate, final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal(amount), dueDate, null, status, null, null);
	}

	private static AgingBucket bucket(final AgingReport report, final AgingRange range) {
		return report.getBuckets().stream().filter(bucket -> bucket.range() == range).findFirst().orElseThrow();
	}

	@Test
	@DisplayName("Buckets the outstanding titles by days overdue as of the query date")
	void bucketsTheOutstandingTitlesByDaysOverdueAsOfTheQueryDate() {
		final LocalDate asOf = TODAY.plusDays(10);
		final Receivable recent = receivable("100.00", asOf.minusDays(5), ReceivableStatus.OPEN);
		final Receivable old = receivable("50.00", asOf.minusDays(120), ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findOutstandingByCustomerDueUntil(null, asOf)).thenReturn(List.of(recent, old));
		when(settlementRepositoryPort.findByReceivableIds(any())).thenReturn(List.of());

		final AgingReport report = service.execute(new GetAgingListQuery(null, null, asOf));

		assertThat(report.getAsOfDate()).isEqualTo(asOf);
		assertThat(bucket(report, AgingRange.UP_TO_30).total()).isEqualByComparingTo("100.00");
		assertThat(bucket(report, AgingRange.OVER_90).total()).isEqualByComparingTo("50.00");
		assertThat(report.getTitleCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("Uses today as the query date when none is given")
	void defaultsTheQueryDateToToday() {
		final Receivable overdue = receivable("10.00", TODAY.minusDays(45), ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findOutstandingByCustomerDueUntil(null, TODAY)).thenReturn(List.of(overdue));
		when(settlementRepositoryPort.findByReceivableIds(any())).thenReturn(List.of());

		final AgingReport report = service.execute(new GetAgingListQuery(null, null, null));

		assertThat(report.getAsOfDate()).isEqualTo(TODAY);
		assertThat(bucket(report, AgingRange.FROM_31_TO_60).total()).isEqualByComparingTo("10.00");
	}

	@Test
	@DisplayName("Counts a partially settled title only for what is still owed")
	void partiallySettledTitleCountsForWhatIsStillOwed() {
		final Receivable partial = receivable("100.00", TODAY.minusDays(70), ReceivableStatus.PARTIALLY_SETTLED);
		final Settlement payment = Settlement.manual(SettlementId.of(UUID.randomUUID()), partial.getId(),
				new BigDecimal("40.00"), new BigDecimal("3.00"), null, null, null, NOW);
		when(receivableRepositoryPort.findOutstandingByCustomerDueUntil(null, TODAY)).thenReturn(List.of(partial));
		when(settlementRepositoryPort.findByReceivableIds(List.of(partial.getId()))).thenReturn(List.of(payment));

		final AgingReport report = service.execute(new GetAgingListQuery(null, null, null));

		assertThat(bucket(report, AgingRange.FROM_61_TO_90).total()).isEqualByComparingTo("60.00");
	}

	@Test
	@DisplayName("Leaves out settled, renegotiated and cancelled titles even if the repository returns them")
	void settledRenegotiatedAndCancelledTitlesAreLeftOutEvenIfTheRepositoryReturnsThem() {
		when(receivableRepositoryPort.findOutstandingByCustomerDueUntil(null, TODAY)).thenReturn(List.of(
				receivable("10.00", TODAY.minusDays(5), ReceivableStatus.SETTLED),
				receivable("20.00", TODAY.minusDays(5), ReceivableStatus.RENEGOTIATED),
				receivable("30.00", TODAY.minusDays(5), ReceivableStatus.CANCELLED)));
		when(settlementRepositoryPort.findByReceivableIds(any())).thenReturn(List.of());

		final AgingReport report = service.execute(new GetAgingListQuery(null, null, null));

		assertThat(report.getTitleCount()).isZero();
	}

	@Test
	@DisplayName("Filters the aging list by customer")
	void filtersByCustomer() {
		final UUID customerId = UUID.randomUUID();
		when(receivableRepositoryPort.findOutstandingByCustomerDueUntil(customerId, TODAY)).thenReturn(List.of());

		final AgingReport report = service.execute(new GetAgingListQuery(customerId, null, null));

		assertThat(report.getTitleCount()).isZero();
		verify(receivableRepositoryPort).findOutstandingByCustomerDueUntil(customerId, TODAY);
	}

	@Test
	@DisplayName("Returns an empty report for a cost center because receivables are not charged to one")
	void costCenterYieldsAnEmptyReportAsReceivablesAreNotChargedToOne() {
		final AgingReport report = service.execute(new GetAgingListQuery(null, UUID.randomUUID(), null));

		assertThat(report.getBuckets()).hasSize(4);
		assertThat(report.getTitleCount()).isZero();
		verifyNoInteractions(receivableRepositoryPort, settlementRepositoryPort);
	}
}
