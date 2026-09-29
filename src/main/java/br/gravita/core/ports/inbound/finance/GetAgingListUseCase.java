package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.AgingReport;

/**
 * UC-M8-08: the delinquency report, read-only. Buckets what is still owed on
 * the open titles ({@code OPEN} or {@code PARTIALLY_SETTLED}) by days overdue
 * as of the query date: 0-30, 31-60, 61-90 and more than 90 days. Settled,
 * renegotiated and cancelled titles are not part of it.
 */
public interface GetAgingListUseCase {

	AgingReport execute(GetAgingListQuery query);
}
