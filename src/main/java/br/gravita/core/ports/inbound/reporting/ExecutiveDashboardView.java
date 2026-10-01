package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Read model composed from the sales, inventory, finance and tax read ports for the dashboard screen (doc §10.1).
 * It owns no state; every field is derived at request time (or served from a short-lived cache).
 * {@code from}/{@code to} are the inclusive bounds of the selected {@code period}, which drives {@code margin},
 * {@code topProducts} and the reconciliation inside {@code revenue}.
 */
public record ExecutiveDashboardView(DashboardPeriod period, LocalDate from, LocalDate to, Revenue revenue,
		Margin margin, Delinquency delinquency, List<CriticalStockItem> criticalStock, TopProducts topProducts,
		TargetProgress targets) {

	/**
	 * Faturamento of the current day, week and month, each against the prior period over the same elapsed span.
	 * {@code invoicedTotal} is the tax-side total for the selected period and {@code reconciliationDifference}
	 * the sales total minus it (non-zero when invoiced orders lack an issued fiscal document).
	 */
	public record Revenue(PeriodComparison day, PeriodComparison week, PeriodComparison month,
			BigDecimal invoicedTotal, BigDecimal reconciliationDifference) {
	}

	/** {@code variationPercent} is {@code null} when the prior period had no revenue to compare to. */
	public record PeriodComparison(BigDecimal current, BigDecimal previous, BigDecimal variationPercent) {
	}

	/** {@code grossMarginPercent} is {@code null} when there was no revenue in the period. */
	public record Margin(BigDecimal revenue, BigDecimal cmv, BigDecimal grossMargin, BigDecimal grossMarginPercent) {
	}

	/** Open titles past due; {@code aging} splits the same total by days overdue. */
	public record Delinquency(BigDecimal totalOverdue, int titleCount, AgingBuckets aging) {
	}

	public record AgingBuckets(BigDecimal upTo30Days, BigDecimal from31To60Days, BigDecimal over60Days) {
	}

	public enum CriticalStockReason {
		BELOW_MINIMUM, NEAR_EXPIRY
	}

	/**
	 * One row of the clickable critical-stock list. A {@code BELOW_MINIMUM} row carries {@code available} and
	 * {@code minimum} (summed over warehouses); a {@code NEAR_EXPIRY} row carries the lot's data.
	 */
	public record CriticalStockItem(UUID productId, CriticalStockReason reason, BigDecimal available,
			BigDecimal minimum, UUID warehouseId, String lotCode, LocalDate expiryDate) {
	}

	public record TopProducts(List<TopProduct> byQuantity, List<TopProduct> byValue) {
	}

	public record TopProduct(UUID productId, BigDecimal quantity, BigDecimal value) {
	}

	/**
	 * Progress for the current month. The company's target is the sum of its salespeople's targets and its
	 * achievement every invoiced order of the month; {@code percentComplete} is {@code null} without a target.
	 */
	public record TargetProgress(YearMonth month, Target company, List<SalespersonTarget> salespeople) {
	}

	public record Target(BigDecimal valueTarget, BigDecimal valueAchieved, BigDecimal percentComplete) {
	}

	public record SalespersonTarget(UUID salespersonId, Target target) {
	}
}
