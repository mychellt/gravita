package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Receivable;

/**
 * UC-M8-02: creates a one-off {@code Receivable} not tied to an invoice
 * (e.g. a standalone charge). The customer must already be registered in
 * {@code masterdata}; the created title starts as {@code MANUAL} origin,
 * {@code OPEN} status.
 */
public interface CreateManualReceivableUseCase {

	Receivable execute(CreateManualReceivableCommand command);
}
