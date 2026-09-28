package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Settlement;

/**
 * UC-M8-06: a user-entered baixa (full or partial) of an open receivable, e.g.
 * for a cash payment. Creates a {@code MANUAL} {@link Settlement}, moves the
 * receivable to {@code PARTIALLY_SETTLED} or {@code SETTLED} and refreshes the
 * customer's credit status in {@code masterdata}.
 */
public interface SettleTitleManuallyUseCase {

	Settlement execute(SettleTitleCommand command);
}
