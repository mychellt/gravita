package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.DashboardPeriod;
import br.gravita.core.ports.inbound.reporting.DashboardQuery;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.CriticalStockReason;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort.OverdueBalance;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.StockAlert;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.SalespersonAchievement;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.usercases.reporting.GetExecutiveDashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetExecutiveDashboardServiceTest {

	// Wednesday: the week is 2026-09-14..16 (prior 09-07..09), the month 09-01..16 (prior 08-01..16).
	private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

	private final SalesReadModelPort sales = mock(SalesReadModelPort.class);
	private final InventoryReadModelPort inventory = mock(InventoryReadModelPort.class);
	private final FinanceReadModelPort finance = mock(FinanceReadModelPort.class);
	private final TaxReadModelPort tax = mock(TaxReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private GetExecutiveDashboardService service;

	@BeforeEach
	void setUp() {
		service = new GetExecutiveDashboardService(sales, inventory, finance, tax, permissions,
				Clock.fixed(NOW, ZoneOffset.UTC), Runnable::run);
		when(permissions.canView(user, "dashboard")).thenReturn(true);
		when(sales.dailyRevenue(any(), any(), any())).thenReturn(Map.of());
		when(sales.productSales(any(), any(), any())).thenReturn(List.of());
		when(sales.targetAchievement(any(), any())).thenReturn(List.of());
		when(inventory.costOfGoodsSold(any())).thenReturn(BigDecimal.ZERO);
		when(inventory.criticalStock(any(), anyInt(), any())).thenReturn(List.of());
		when(finance.overdueReceivables(any(), any())).thenReturn(List.of());
		when(tax.invoicedTotal(any(), any(), any())).thenReturn(BigDecimal.ZERO);
	}

	@Test
	void refusesAUserWhoseProfileCannotViewTheDashboardWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "dashboard")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new DashboardQuery(stranger, DashboardPeriod.MONTH, null)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(sales, inventory, finance, tax);
	}

	@Test
	void comparesDayWeekAndMonthRevenueToTheSameSpanOfThePriorPeriod() {
		when(sales.dailyRevenue(eq(LocalDate.of(2026, 8, 1)), eq(TODAY), any())).thenReturn(Map.of(
				TODAY, new BigDecimal("100"), TODAY.minusDays(1), new BigDecimal("50"),
				LocalDate.of(2026, 9, 14), new BigDecimal("200"),
				LocalDate.of(2026, 9, 10), new BigDecimal("999"),
				LocalDate.of(2026, 9, 9), new BigDecimal("40"), LocalDate.of(2026, 9, 8), new BigDecimal("60"),
				LocalDate.of(2026, 9, 2), new BigDecimal("300"),
				LocalDate.of(2026, 8, 5), new BigDecimal("1000"),
				LocalDate.of(2026, 8, 17), new BigDecimal("5000")));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));

		assertThat(view.revenue().day().current()).isEqualByComparingTo("100");
		assertThat(view.revenue().day().previous()).isEqualByComparingTo("50");
		assertThat(view.revenue().day().variationPercent()).isEqualByComparingTo("100.00");
		assertThat(view.revenue().week().current()).isEqualByComparingTo("350");
		assertThat(view.revenue().week().previous()).isEqualByComparingTo("100");
		assertThat(view.revenue().week().variationPercent()).isEqualByComparingTo("250.00");
		// 08-17 is past the span of the prior month matching the 16 elapsed days of September.
		assertThat(view.revenue().month().current()).isEqualByComparingTo("1749");
		assertThat(view.revenue().month().previous()).isEqualByComparingTo("1000");
		assertThat(view.revenue().month().variationPercent()).isEqualByComparingTo("74.90");
	}

	@Test
	void leavesTheVariationEmptyWhenThePriorPeriodHadNoRevenue() {
		when(sales.dailyRevenue(any(), any(), any())).thenReturn(Map.of(TODAY, new BigDecimal("100")));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.DAY, null));

		assertThat(view.revenue().day().previous()).isEqualByComparingTo("0");
		assertThat(view.revenue().day().variationPercent()).isNull();
	}

	@Test
	void computesCmvAndGrossMarginForTheSelectedPeriodAndReconcilesWithInvoicedTotal() {
		UUID product = UUID.randomUUID();
		when(sales.dailyRevenue(any(), any(), any()))
				.thenReturn(Map.of(TODAY, new BigDecimal("1000"), TODAY.minusDays(1), new BigDecimal("500")));
		when(sales.productSales(eq(TODAY.minusDays(2)), eq(TODAY), any()))
				.thenReturn(List.of(new ProductSales(product, new BigDecimal("3"), new BigDecimal("1500"))));
		when(inventory.costOfGoodsSold(List.of(new InventoryReadModelPort.SoldQuantity(product, new BigDecimal("3")))))
				.thenReturn(new BigDecimal("900"));
		when(tax.invoicedTotal(eq(TODAY.minusDays(2)), eq(TODAY), any())).thenReturn(new BigDecimal("1400"));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.WEEK, null));

		assertThat(view.from()).isEqualTo(TODAY.minusDays(2));
		assertThat(view.to()).isEqualTo(TODAY);
		assertThat(view.margin().revenue()).isEqualByComparingTo("1500");
		assertThat(view.margin().cmv()).isEqualByComparingTo("900");
		assertThat(view.margin().grossMargin()).isEqualByComparingTo("600");
		assertThat(view.margin().grossMarginPercent()).isEqualByComparingTo("40.00");
		assertThat(view.revenue().invoicedTotal()).isEqualByComparingTo("1400");
		assertThat(view.revenue().reconciliationDifference()).isEqualByComparingTo("100");
	}

	@Test
	void leavesTheMarginPercentEmptyWithoutRevenue() {
		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.DAY, null));

		assertThat(view.margin().grossMarginPercent()).isNull();
	}

	@Test
	void splitsOverdueBalancesIntoAgingBucketsAndIgnoresTitlesNotPastDue() {
		when(finance.overdueReceivables(eq(TODAY), any())).thenReturn(List.of(
				new OverdueBalance(TODAY.minusDays(1), new BigDecimal("10")),
				new OverdueBalance(TODAY.minusDays(30), new BigDecimal("20")),
				new OverdueBalance(TODAY.minusDays(31), new BigDecimal("30")),
				new OverdueBalance(TODAY.minusDays(60), new BigDecimal("40")),
				new OverdueBalance(TODAY.minusDays(61), new BigDecimal("50")),
				new OverdueBalance(TODAY.minusDays(200), new BigDecimal("60")),
				new OverdueBalance(TODAY, new BigDecimal("999")),
				new OverdueBalance(TODAY.minusDays(5), BigDecimal.ZERO)));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));

		assertThat(view.delinquency().totalOverdue()).isEqualByComparingTo("210");
		assertThat(view.delinquency().titleCount()).isEqualTo(6);
		assertThat(view.delinquency().aging().upTo30Days()).isEqualByComparingTo("30");
		assertThat(view.delinquency().aging().from31To60Days()).isEqualByComparingTo("70");
		assertThat(view.delinquency().aging().over60Days()).isEqualByComparingTo("110");
	}

	@Test
	void listsBelowMinimumProductsBeforeNearExpiryLotsOrderedByExpiry() {
		UUID low = UUID.randomUUID();
		UUID lotA = UUID.randomUUID();
		UUID lotB = UUID.randomUUID();
		UUID warehouse = UUID.randomUUID();
		when(inventory.criticalStock(eq(TODAY), eq(30), any())).thenReturn(List.of(
				new StockAlert(lotA, null, null, warehouse, "L2", TODAY.plusDays(20)),
				new StockAlert(low, new BigDecimal("2"), new BigDecimal("10"), null, null, null),
				new StockAlert(lotB, null, null, warehouse, "L1", TODAY.minusDays(3))));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));

		assertThat(view.criticalStock()).extracting(item -> item.productId()).containsExactly(low, lotB, lotA);
		assertThat(view.criticalStock()).extracting(item -> item.reason()).containsExactly(
				CriticalStockReason.BELOW_MINIMUM, CriticalStockReason.NEAR_EXPIRY, CriticalStockReason.NEAR_EXPIRY);
		assertThat(view.criticalStock().get(0).available()).isEqualByComparingTo("2");
		assertThat(view.criticalStock().get(0).minimum()).isEqualByComparingTo("10");
		assertThat(view.criticalStock().get(1).lotCode()).isEqualTo("L1");
	}

	@Test
	void ranksTheTopTenProductsByQuantityAndByValueSeparately() {
		List<ProductSales> products = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			// quantity rises with i while value falls with i, so the two rankings are mirror images.
			products.add(new ProductSales(new UUID(0, i), BigDecimal.valueOf(i), BigDecimal.valueOf(100 - i)));
		}
		when(sales.productSales(any(), any(), any())).thenReturn(products);

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));

		assertThat(view.topProducts().byQuantity()).hasSize(10);
		assertThat(view.topProducts().byQuantity()).extracting(p -> p.productId())
				.containsExactly(new UUID(0, 12), new UUID(0, 11), new UUID(0, 10), new UUID(0, 9), new UUID(0, 8),
						new UUID(0, 7), new UUID(0, 6), new UUID(0, 5), new UUID(0, 4), new UUID(0, 3));
		assertThat(view.topProducts().byValue()).hasSize(10);
		assertThat(view.topProducts().byValue().get(0).productId()).isEqualTo(new UUID(0, 1));
		assertThat(view.topProducts().byValue().get(9).productId()).isEqualTo(new UUID(0, 10));
	}

	@Test
	void reportsTargetProgressPerSalespersonAndForTheCompany() {
		UUID ana = UUID.randomUUID();
		UUID bruno = UUID.randomUUID();
		UUID carla = UUID.randomUUID();
		when(sales.targetAchievement(eq(java.time.YearMonth.of(2026, 9)), any())).thenReturn(List.of(
				new SalespersonAchievement(ana, new BigDecimal("1000"), new BigDecimal("600")),
				new SalespersonAchievement(bruno, new BigDecimal("1000"), new BigDecimal("1200")),
				new SalespersonAchievement(carla, BigDecimal.ZERO, new BigDecimal("200"))));

		ExecutiveDashboardView view = service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));

		assertThat(view.targets().month()).isEqualTo(java.time.YearMonth.of(2026, 9));
		assertThat(view.targets().company().valueTarget()).isEqualByComparingTo("2000");
		assertThat(view.targets().company().valueAchieved()).isEqualByComparingTo("2000");
		assertThat(view.targets().company().percentComplete()).isEqualByComparingTo("100.00");
		assertThat(view.targets().salespeople()).extracting(s -> s.salespersonId()).containsExactly(bruno, ana, carla);
		assertThat(view.targets().salespeople().get(0).target().percentComplete()).isEqualByComparingTo("120.00");
		assertThat(view.targets().salespeople().get(1).target().percentComplete()).isEqualByComparingTo("60.00");
		assertThat(view.targets().salespeople().get(2).target().percentComplete()).isNull();
	}

	@Test
	void passesTheCompanyToEveryReadPort() {
		UUID company = UUID.randomUUID();

		service.execute(new DashboardQuery(user, DashboardPeriod.MONTH, company));

		verify(sales).dailyRevenue(any(), any(), eq(company));
		verify(sales).productSales(any(), any(), eq(company));
		verify(sales).targetAchievement(any(), eq(company));
		verify(tax).invoicedTotal(any(), any(), eq(company));
		verify(finance).overdueReceivables(any(), eq(company));
		verify(inventory).criticalStock(any(), anyInt(), eq(company));
	}

	@Test
	void servesRepeatedRequestsFromCacheButStillChecksPermissionEachTime() {
		DashboardQuery query = new DashboardQuery(user, DashboardPeriod.MONTH, null);

		ExecutiveDashboardView first = service.execute(query);
		ExecutiveDashboardView second = service.execute(query);

		assertThat(second).isSameAs(first);
		verify(sales, times(1)).dailyRevenue(any(), any(), any());
		verify(permissions, times(2)).canView(user, "dashboard");
	}

	@Test
	void recomposesOnceTheCacheEntryExpires() {
		var clock = new MutableClock(NOW);
		service = new GetExecutiveDashboardService(sales, inventory, finance, tax, permissions, clock, Runnable::run);
		DashboardQuery query = new DashboardQuery(user, DashboardPeriod.MONTH, null);

		service.execute(query);
		clock.advanceSeconds(61);
		service.execute(query);

		verify(sales, times(2)).dailyRevenue(any(), any(), any());
	}

	/** A clock the test can move forward. */
	private static final class MutableClock extends Clock {
		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advanceSeconds(long seconds) {
			now = now.plusSeconds(seconds);
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
