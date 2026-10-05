package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummary;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummaryQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.PurchasingReadModelPort;
import br.gravita.core.ports.outbound.reporting.PurchasingReadModelPort.PurchasedOrder;
import br.gravita.core.usercases.reporting.GetSupplierPurchaseSummaryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetSupplierPurchaseSummaryServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final LocalDate FROM = LocalDate.of(2028, 2, 1);
	private static final LocalDate TO = LocalDate.of(2028, 2, 29);

	private final PurchasingReadModelPort purchasing = mock(PurchasingReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private final UUID company = UUID.randomUUID();
	private GetSupplierPurchaseSummaryService service;

	@BeforeEach
	void setUp() {
		service = new GetSupplierPurchaseSummaryService(purchasing, permissions);
		when(permissions.canView(user, "purchases-by-supplier")).thenReturn(true);
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of());
	}

	@DisplayName("Refuses a user whose profile cannot view the report, without reading any data")
	@Test
	void refusesAUserWhoseProfileCannotViewTheReportWithoutReadingAnything() {
		final UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "purchases-by-supplier")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new SupplierPurchaseSummaryQuery(stranger, PERIOD, company)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(purchasing);
	}

	@DisplayName("Reads the whole month of the requested period")
	@Test
	void readsTheWholeMonthOfTheRequestedPeriod() {
		service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, company));

		verify(purchasing).purchasedOrders(FROM, TO, company);
	}

	@DisplayName("Reports nothing when nothing was bought in the period")
	@Test
	void reportsNothingWhenNothingWasBoughtInThePeriod() {
		assertThat(service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null))).isEmpty();
	}

	@DisplayName("Sums the volume and value of a supplier's orders")
	@Test
	void sumsTheVolumeAndValueOfASuppliersOrders() {
		final UUID supplier = UUID.randomUUID();
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of(
				order(supplier, "2028-02-03", "10", "50.00"), order(supplier, "2028-02-20", "4.5", "10.255")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		assertThat(report).singleElement().satisfies(summary -> {
			assertThat(summary.supplier()).isEqualTo(supplier);
			assertThat(summary.volume()).isEqualByComparingTo("14.5");
			assertThat(summary.value()).isEqualByComparingTo("60.26");
		});
	}

	@DisplayName("Averages the days from order to every confirmed delivery")
	@Test
	void averagesTheDaysFromTheOrderToEveryConfirmedDelivery() {
		final UUID supplier = UUID.randomUUID();
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of(
				order(supplier, "2028-02-03", "10", "50", "2028-02-10"),
				// Delivered in two parts: both deliveries count, one 4 days after the order and one 10 days after.
				order(supplier, "2028-02-20", "4", "10", "2028-02-24", "2028-03-01")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		// (7 + 4 + 10) / 3 deliveries
		assertThat(report).singleElement()
				.satisfies(summary -> assertThat(summary.averageLeadTimeDays()).isEqualByComparingTo("7"));
	}

	@DisplayName("Rounds the average lead time to two decimal places")
	@Test
	void roundsTheAverageLeadTimeToTwoDecimals() {
		final UUID supplier = UUID.randomUUID();
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of(
				order(supplier, "2028-02-01", "1", "1", "2028-02-02"),
				order(supplier, "2028-02-01", "1", "1", "2028-02-02"),
				order(supplier, "2028-02-01", "1", "1", "2028-02-03")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		assertThat(report).singleElement()
				.satisfies(summary -> assertThat(summary.averageLeadTimeDays()).isEqualByComparingTo("1.33"));
	}

	@DisplayName("Leaves the lead time empty for a supplier whose orders were not received yet")
	@Test
	void leavesTheLeadTimeEmptyForASupplierWhoseOrdersWereNotReceivedYet() {
		final UUID supplier = UUID.randomUUID();
		when(purchasing.purchasedOrders(any(), any(), any()))
				.thenReturn(List.of(order(supplier, "2028-02-03", "10", "50")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		assertThat(report).singleElement().satisfies(summary -> {
			assertThat(summary.value()).isEqualByComparingTo("50");
			assertThat(summary.averageLeadTimeDays()).isNull();
		});
	}

	@DisplayName("Ignores other suppliers' orders when averaging a supplier's lead time")
	@Test
	void ignoresTheOrdersOfOtherSuppliersWhenAveragingTheLeadTime() {
		final UUID fast = UUID.fromString("00000000-0000-0000-0000-000000000001");
		final UUID slow = UUID.fromString("00000000-0000-0000-0000-000000000002");
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of(
				order(fast, "2028-02-03", "1", "10", "2028-02-04"), order(slow, "2028-02-03", "1", "10", "2028-02-23")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		assertThat(report).extracting(SupplierPurchaseSummary::supplier).containsExactly(fast, slow);
		assertThat(report).extracting(SupplierPurchaseSummary::averageLeadTimeDays)
				.satisfiesExactly(days -> assertThat(days).isEqualByComparingTo("1"),
						days -> assertThat(days).isEqualByComparingTo("20"));
	}

	@DisplayName("Ranks the suppliers by value bought, largest first")
	@Test
	void ranksTheSuppliersByValueBoughtLargestFirst() {
		final UUID small = UUID.fromString("00000000-0000-0000-0000-000000000001");
		final UUID big = UUID.fromString("00000000-0000-0000-0000-000000000002");
		final UUID tieA = UUID.fromString("00000000-0000-0000-0000-000000000003");
		final UUID tieB = UUID.fromString("00000000-0000-0000-0000-000000000004");
		when(purchasing.purchasedOrders(any(), any(), any())).thenReturn(List.of(order(tieB, "2028-02-03", "1", "20"),
				order(small, "2028-02-03", "1", "5"), order(big, "2028-02-03", "1", "90"),
				order(tieA, "2028-02-03", "1", "20")));

		final List<SupplierPurchaseSummary> report = service.execute(new SupplierPurchaseSummaryQuery(user, PERIOD, null));

		assertThat(report).extracting(SupplierPurchaseSummary::supplier).containsExactly(big, tieA, tieB, small);
	}

	private PurchasedOrder order(final UUID supplier, final String orderedOn, final String quantity, final String value,
			final String... receivedOn) {
		return new PurchasedOrder(supplier, LocalDate.parse(orderedOn), new BigDecimal(quantity),
				new BigDecimal(value), Arrays.stream(receivedOn).map(LocalDate::parse).toList());
	}
}
