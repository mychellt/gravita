package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only view of the invoiced sales the reporting context needs; revenue is the net total of invoiced orders. */
public interface SalesReadModelPort {

	/** Revenue per invoicing day over {@code [from, to]} (inclusive); days without sales are absent. */
	Map<LocalDate, BigDecimal> dailyRevenue(LocalDate from, LocalDate to, UUID companyId);

	/** Quantity and value sold per product over {@code [from, to]} (inclusive), in no particular order. */
	List<ProductSales> productSales(LocalDate from, LocalDate to, UUID companyId);

	/** Value invoiced per customer over {@code [from, to]} (inclusive), in no particular order. */
	List<CustomerSales> customerSales(LocalDate from, LocalDate to, UUID companyId);

	/**
	 * Per salesperson with a target or invoiced orders in {@code month}: what was targeted (zero when none was set)
	 * and what was invoiced.
	 */
	List<SalespersonAchievement> targetAchievement(YearMonth month, UUID companyId);

	/**
	 * Commissions of the orders invoiced over {@code [from, to]} (inclusive), in no particular order; a commission
	 * belongs to the period its order was invoiced in. A {@code null} {@code salespersonId} does not restrict.
	 */
	List<CommissionRecord> commissions(LocalDate from, LocalDate to, UUID salespersonId);

	record ProductSales(UUID productId, BigDecimal quantity, BigDecimal value) {
	}

	record CustomerSales(UUID customerId, BigDecimal value) {
	}

	record SalespersonAchievement(UUID salespersonId, BigDecimal valueTarget, BigDecimal valueAchieved) {
	}

	record CommissionRecord(UUID salespersonId, UUID productId, UUID orderId, BigDecimal rate, BigDecimal amount) {
	}
}
