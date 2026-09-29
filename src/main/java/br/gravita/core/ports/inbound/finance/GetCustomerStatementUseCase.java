package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CustomerStatement;

/**
 * UC-M8-09: the customer's statement, read-only. Combines all their titles,
 * the settlements applied to them and their renegotiations, in chronological
 * order, with the open balance still owed on the {@code OPEN} and
 * {@code PARTIALLY_SETTLED} titles. The balance is the same position
 * M1's customer credit status is derived from.
 */
public interface GetCustomerStatementUseCase {

	CustomerStatement execute(GetCustomerStatementQuery query);
}
