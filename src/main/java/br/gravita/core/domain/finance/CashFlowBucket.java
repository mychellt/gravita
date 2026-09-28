package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One period of a {@link CashFlowProjection}. {@code balance} is the running
 * balance at the end of the period: the projection's opening balance plus the
 * net (in minus out, realized and projected) of this and every earlier period.
 */
public record CashFlowBucket(LocalDate periodStart, LocalDate periodEnd, BigDecimal realizedInflow,
		BigDecimal realizedOutflow, BigDecimal projectedInflow, BigDecimal projectedOutflow, BigDecimal balance) {

	public BigDecimal net() {
		return realizedInflow.add(projectedInflow).subtract(realizedOutflow).subtract(projectedOutflow);
	}
}
