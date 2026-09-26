package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.CashMovement;

public interface CashMovementRepositoryPort {

	CashMovement save(CashMovement cashMovement);
}
