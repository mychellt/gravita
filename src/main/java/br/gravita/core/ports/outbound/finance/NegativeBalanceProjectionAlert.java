package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashFlowGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A cash-flow projection whose balance goes negative: {@code firstNegativePeriodStart}
 * is the start of the first period (of the given {@code granularity}) closing
 * negative and {@code lowestBalance} the lowest closing balance from today on.
 */
public record NegativeBalanceProjectionAlert(CashFlowFilter filter, CashFlowGranularity granularity,
		LocalDate firstNegativePeriodStart, BigDecimal lowestBalance) {
}
