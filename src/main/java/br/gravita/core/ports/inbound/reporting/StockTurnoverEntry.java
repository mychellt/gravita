package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One product's stock turnover in the period: the quantity issued divided by the average stock held, so 2 means the
 * average stock was sold twice over. {@code turnoverRate} is {@code null} when no stock was held at either end of
 * the period, so there is no average to compare to. {@code stalledFlag} marks a product that held stock but issued
 * none of it. It is a read model derived at request time.
 */
public record StockTurnoverEntry(UUID product, BigDecimal turnoverRate, boolean stalledFlag) {
}
