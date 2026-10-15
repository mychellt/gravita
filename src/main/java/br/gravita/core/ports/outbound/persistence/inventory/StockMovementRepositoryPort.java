package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepositoryPort {
	StockMovement save(StockMovement movement);

	/** Movements recorded at or after {@code from}, the window stock levels are rebuilt from. */
	List<StockMovement> findByTimestampGreaterThanEqual(Instant from);

	/** The warehouse's movements matching the query's filters, newest first. */
	Page<StockMovement> search(UUID warehouseId, ListStockMovementsQuery query);
}
