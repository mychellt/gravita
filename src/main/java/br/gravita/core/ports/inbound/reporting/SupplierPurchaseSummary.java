package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What was bought from one supplier in the period: {@code volume} is the quantity ordered, {@code value} what it
 * cost at the ordered unit prices, and {@code averageLeadTimeDays} the average number of days from placing an order
 * to a delivery of it being confirmed. The lead time is {@code null} when none of the supplier's orders of the period
 * has been received yet. It is a read model derived at request time.
 */
public record SupplierPurchaseSummary(UUID supplier, BigDecimal volume, BigDecimal value,
		BigDecimal averageLeadTimeDays) {
}
