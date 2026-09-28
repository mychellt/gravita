package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CashMovement;

/**
 * UC-M8-19: records a transfer between the back office's internal cash box and
 * the bank ({@code TO_BANK} / {@code FROM_BANK}) and updates the box balance.
 * Not a sale, and independent of any PDV register balance (M3).
 */
public interface RecordInternalCashMovementUseCase {

	CashMovement execute(RecordInternalCashMovementCommand command);
}
