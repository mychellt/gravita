package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.DashboardPeriod;
import br.gravita.core.ports.inbound.reporting.DashboardQuery;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.AgingBuckets;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.CriticalStockItem;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.CriticalStockReason;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.Delinquency;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.Margin;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.PeriodComparison;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.Revenue;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.SalespersonTarget;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.Target;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.TargetProgress;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.TopProduct;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.TopProducts;
import br.gravita.core.ports.inbound.reporting.GetExecutiveDashboardUseCase;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort.OverdueBalance;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.SoldQuantity;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.StockAlert;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.SalespersonAchievement;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fans out to the sales, inventory, finance and tax read ports in parallel and composes their answers (doc §10.1).
 * To stay inside the module's 3-second load budget the composed view is kept for {@link #CACHE_TTL} per
 * (company, period, day); visibility is checked on every call, before the cache is consulted.
 */
@UseCase
public class GetExecutiveDashboardService implements GetExecutiveDashboardUseCase {

	static final String SCREEN = "dashboard";
	static final int NEAR_EXPIRY_DAYS = 30;
	static final int TOP_PRODUCTS_SIZE = 10;
	static final Duration CACHE_TTL = Duration.ofSeconds(60);

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final SalesReadModelPort salesReadModelPort;
	private final InventoryReadModelPort inventoryReadModelPort;
	private final FinanceReadModelPort financeReadModelPort;
	private final TaxReadModelPort taxReadModelPort;
	private final PermissionCheckPort permissionCheckPort;
	private final Clock clock;
	private final Executor executor;
	private final Map<CacheKey, CachedView> cache = new ConcurrentHashMap<>();

	@Autowired
	public GetExecutiveDashboardService(SalesReadModelPort salesReadModelPort,
			InventoryReadModelPort inventoryReadModelPort, FinanceReadModelPort financeReadModelPort,
			TaxReadModelPort taxReadModelPort, PermissionCheckPort permissionCheckPort) {
		this(salesReadModelPort, inventoryReadModelPort, financeReadModelPort, taxReadModelPort,
				permissionCheckPort, Clock.systemDefaultZone(), Executors.newVirtualThreadPerTaskExecutor());
	}

	public GetExecutiveDashboardService(SalesReadModelPort salesReadModelPort,
			InventoryReadModelPort inventoryReadModelPort, FinanceReadModelPort financeReadModelPort,
			TaxReadModelPort taxReadModelPort, PermissionCheckPort permissionCheckPort, Clock clock,
			Executor executor) {
		this.salesReadModelPort = salesReadModelPort;
		this.inventoryReadModelPort = inventoryReadModelPort;
		this.financeReadModelPort = financeReadModelPort;
		this.taxReadModelPort = taxReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
		this.clock = clock;
		this.executor = executor;
	}

	@Override
	public ExecutiveDashboardView execute(DashboardQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the executive dashboard");
		}
		LocalDate today = LocalDate.now(clock);
		CacheKey key = new CacheKey(query.companyId(), query.period(), today);
		Instant now = clock.instant();
		CachedView cached = cache.get(key);
		if (cached != null && now.isBefore(cached.expiresAt())) {
			return cached.view();
		}
		ExecutiveDashboardView view = compose(query, today);
		cache.values().removeIf(entry -> !now.isBefore(entry.expiresAt()));
		cache.put(key, new CachedView(view, now.plus(CACHE_TTL)));
		return view;
	}

	private ExecutiveDashboardView compose(DashboardQuery query, LocalDate today) {
		UUID companyId = query.companyId();
		Window selected = currentWindow(query.period(), today);
		Window dayWindow = currentWindow(DashboardPeriod.DAY, today);
		Window weekWindow = currentWindow(DashboardPeriod.WEEK, today);
		Window monthWindow = currentWindow(DashboardPeriod.MONTH, today);
		LocalDate earliest = List.of(previousWindow(DashboardPeriod.DAY, today).from(),
				previousWindow(DashboardPeriod.WEEK, today).from(), previousWindow(DashboardPeriod.MONTH, today).from())
				.stream().min(Comparator.naturalOrder()).orElseThrow();
		YearMonth month = YearMonth.from(today);

		CompletableFuture<Map<LocalDate, BigDecimal>> dailyRevenue = async(
				() -> salesReadModelPort.dailyRevenue(earliest, today, companyId));
		CompletableFuture<List<ProductSales>> productSales = async(
				() -> salesReadModelPort.productSales(selected.from(), selected.to(), companyId));
		CompletableFuture<BigDecimal> cmv = productSales.thenApplyAsync(sales -> inventoryReadModelPort
				.costOfGoodsSold(sales.stream().map(sale -> new SoldQuantity(sale.productId(), sale.quantity())).toList()),
				executor);
		CompletableFuture<List<SalespersonAchievement>> achievements = async(
				() -> salesReadModelPort.targetAchievement(month, companyId));
		CompletableFuture<BigDecimal> invoiced = async(
				() -> taxReadModelPort.invoicedTotal(selected.from(), selected.to(), companyId));
		CompletableFuture<List<OverdueBalance>> overdue = async(
				() -> financeReadModelPort.overdueReceivables(today, companyId));
		CompletableFuture<List<StockAlert>> stockAlerts = async(
				() -> inventoryReadModelPort.criticalStock(today, NEAR_EXPIRY_DAYS, companyId));

		Map<LocalDate, BigDecimal> revenueByDay = await(dailyRevenue);
		BigDecimal selectedRevenue = sum(revenueByDay, selected);
		Revenue revenue = new Revenue(comparison(revenueByDay, dayWindow, DashboardPeriod.DAY, today),
				comparison(revenueByDay, weekWindow, DashboardPeriod.WEEK, today),
				comparison(revenueByDay, monthWindow, DashboardPeriod.MONTH, today), await(invoiced),
				selectedRevenue.subtract(await(invoiced)));
		List<ProductSales> sales = await(productSales);
		return new ExecutiveDashboardView(query.period(), selected.from(), selected.to(), revenue,
				margin(selectedRevenue, await(cmv)), delinquency(await(overdue), today), criticalStock(await(stockAlerts)),
				topProducts(sales), targetProgress(month, await(achievements)));
	}

	private PeriodComparison comparison(Map<LocalDate, BigDecimal> revenueByDay, Window current,
			DashboardPeriod period, LocalDate today) {
		BigDecimal currentTotal = sum(revenueByDay, current);
		BigDecimal previousTotal = sum(revenueByDay, previousWindow(period, today));
		BigDecimal variation = percent(currentTotal.subtract(previousTotal), previousTotal);
		return new PeriodComparison(currentTotal, previousTotal, variation);
	}

	private Margin margin(BigDecimal revenue, BigDecimal cmv) {
		BigDecimal grossMargin = revenue.subtract(cmv);
		return new Margin(revenue, cmv, grossMargin, percent(grossMargin, revenue));
	}

	/** Every title here is already past due: ≤30 days overdue, 31–60, and more than 60. */
	private Delinquency delinquency(List<OverdueBalance> balances, LocalDate today) {
		BigDecimal upTo30 = BigDecimal.ZERO;
		BigDecimal from31To60 = BigDecimal.ZERO;
		BigDecimal over60 = BigDecimal.ZERO;
		int titles = 0;
		for (OverdueBalance balance : balances) {
			if (balance.outstanding().signum() <= 0) {
				continue;
			}
			long daysOverdue = ChronoUnit.DAYS.between(balance.dueDate(), today);
			if (daysOverdue < 1) {
				continue;
			}
			titles++;
			if (daysOverdue <= 30) {
				upTo30 = upTo30.add(balance.outstanding());
			} else if (daysOverdue <= 60) {
				from31To60 = from31To60.add(balance.outstanding());
			} else {
				over60 = over60.add(balance.outstanding());
			}
		}
		return new Delinquency(upTo30.add(from31To60).add(over60), titles,
				new AgingBuckets(upTo30, from31To60, over60));
	}

	/** Below-minimum products first, then lots by earliest expiry. */
	private List<CriticalStockItem> criticalStock(List<StockAlert> alerts) {
		Comparator<StockAlert> order = Comparator.comparing((StockAlert alert) -> !alert.isBelowMinimum())
				.thenComparing(StockAlert::expiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(StockAlert::productId);
		return alerts.stream().sorted(order)
				.map(alert -> new CriticalStockItem(alert.productId(),
						alert.isBelowMinimum() ? CriticalStockReason.BELOW_MINIMUM : CriticalStockReason.NEAR_EXPIRY,
						alert.available(), alert.minimum(), alert.warehouseId(), alert.lotCode(), alert.expiryDate()))
				.toList();
	}

	private TopProducts topProducts(List<ProductSales> sales) {
		List<TopProduct> products = sales.stream()
				.map(sale -> new TopProduct(sale.productId(), sale.quantity(), sale.value())).toList();
		return new TopProducts(
				top(products, Comparator.comparing(TopProduct::quantity).reversed()),
				top(products, Comparator.comparing(TopProduct::value).reversed()));
	}

	private List<TopProduct> top(List<TopProduct> products, Comparator<TopProduct> ranking) {
		return products.stream().sorted(ranking.thenComparing(TopProduct::productId)).limit(TOP_PRODUCTS_SIZE)
				.toList();
	}

	private TargetProgress targetProgress(YearMonth month, List<SalespersonAchievement> achievements) {
		BigDecimal companyTarget = achievements.stream().map(SalespersonAchievement::valueTarget)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal companyAchieved = achievements.stream().map(SalespersonAchievement::valueAchieved)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<SalespersonTarget> salespeople = achievements.stream()
				.sorted(Comparator.comparing(SalespersonAchievement::valueAchieved).reversed()
						.thenComparing(SalespersonAchievement::salespersonId))
				.map(achievement -> new SalespersonTarget(achievement.salespersonId(),
						target(achievement.valueTarget(), achievement.valueAchieved())))
				.toList();
		return new TargetProgress(month, target(companyTarget, companyAchieved), salespeople);
	}

	private Target target(BigDecimal valueTarget, BigDecimal valueAchieved) {
		return new Target(valueTarget, valueAchieved, percent(valueAchieved, valueTarget));
	}

	/** The current window of {@code period}: from its first day (Monday, for a week) up to {@code today}. */
	static Window currentWindow(DashboardPeriod period, LocalDate today) {
		return switch (period) {
			case DAY -> new Window(today, today);
			case WEEK -> new Window(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
			case MONTH -> new Window(today.withDayOfMonth(1), today);
		};
	}

	/** The prior period over the same elapsed span, so a half-way month is compared to the previous month's first half. */
	static Window previousWindow(DashboardPeriod period, LocalDate today) {
		Window current = currentWindow(period, today);
		return switch (period) {
			case DAY -> new Window(today.minusDays(1), today.minusDays(1));
			case WEEK -> new Window(current.from().minusWeeks(1), current.to().minusWeeks(1));
			case MONTH -> {
				LocalDate from = current.from().minusMonths(1);
				LocalDate sameSpanEnd = from.plusDays(ChronoUnit.DAYS.between(current.from(), current.to()));
				LocalDate lastDay = YearMonth.from(from).atEndOfMonth();
				yield new Window(from, sameSpanEnd.isAfter(lastDay) ? lastDay : sameSpanEnd);
			}
		};
	}

	private BigDecimal sum(Map<LocalDate, BigDecimal> revenueByDay, Window window) {
		return revenueByDay.entrySet().stream()
				.filter(entry -> !entry.getKey().isBefore(window.from()) && !entry.getKey().isAfter(window.to()))
				.map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** {@code part / whole} as a percentage; {@code null} when there is no whole to compare to. */
	private BigDecimal percent(BigDecimal part, BigDecimal whole) {
		if (whole.signum() == 0) {
			return null;
		}
		return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
	}

	private <T> CompletableFuture<T> async(Supplier<T> read) {
		return CompletableFuture.supplyAsync(read, executor);
	}

	private <T> T await(CompletableFuture<T> future) {
		try {
			return future.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof RuntimeException cause) {
				throw cause;
			}
			throw e;
		}
	}

	record Window(LocalDate from, LocalDate to) {
	}

	private record CacheKey(UUID companyId, DashboardPeriod period, LocalDate day) {
	}

	private record CachedView(ExecutiveDashboardView view, Instant expiresAt) {
	}
}
