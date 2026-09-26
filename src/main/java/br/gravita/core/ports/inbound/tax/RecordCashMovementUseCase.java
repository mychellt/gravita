package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.CashMovementId;

public interface RecordCashMovementUseCase {
	CashMovementId execute(RecordCashMovementCommand command);
}
