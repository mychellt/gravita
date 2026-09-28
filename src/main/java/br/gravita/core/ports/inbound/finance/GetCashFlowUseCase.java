package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CashFlowProjection;

/**
 * UC-M8-17: the cash-flow view, read-only. Combines the realized entries and
 * exits (the settlements of receivables and payables) with the open titles
 * projected on their due date, bucketed per day, week or month and filtered by
 * company, branch, bank account and cost center. When the running balance is
 * projected to go negative, {@code NotifyNegativeBalanceProjectionPort} is
 * triggered before the projection is returned.
 */
public interface GetCashFlowUseCase {

	CashFlowProjection execute(GetCashFlowQuery query);
}
