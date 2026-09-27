package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockMovement;

public interface StockMovementRepositoryPort {
	StockMovement save(StockMovement movement);
}
