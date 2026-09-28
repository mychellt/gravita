package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Renegotiation;

/**
 * UC-M8-07: converts one or more overdue receivables of a customer into a new
 * installment plan. The originals become {@code RENEGOTIATED} (and so leave the
 * aging), the plan's installments are created as {@code OPEN} receivables, and
 * the returned {@link Renegotiation} links the two sides. Afterwards the
 * customer's credit status is refreshed in {@code masterdata}. All or nothing.
 */
public interface RenegotiateTitleUseCase {

	Renegotiation execute(RenegotiateTitleCommand command);
}
