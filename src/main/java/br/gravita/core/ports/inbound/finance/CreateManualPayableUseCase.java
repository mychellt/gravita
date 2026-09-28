package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-10: creates a one-off {@code Payable} not tied to a purchase receipt
 * (e.g. rent, utilities). The supplier is optional; when given it must already
 * be registered in {@code masterdata}. The created title starts as
 * {@code MANUAL} origin, {@code OPEN} status.
 */
public interface CreateManualPayableUseCase {

	Payable execute(CreateManualPayableCommand command);
}
