package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockMovement;

/**
 * Append-only ledger - intentionally exposes no update/delete method
 * (UC-M5-02, AC2).
 */
public interface StockMovementRepositoryPort {
	StockMovement save(StockMovement movement);
}
