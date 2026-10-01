package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One product or customer (per the query's type) on the curve, ranked by revenue. Shares are percentages of the
 * period's total revenue; {@code cumulativeShare} includes this entry. It is a read model derived at request time.
 */
public record AbcCurveEntry(UUID entityId, BigDecimal revenue, BigDecimal revenueShare, BigDecimal cumulativeShare,
		AbcClass abcClass) {
}
