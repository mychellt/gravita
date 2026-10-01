package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockMovement;
import java.time.Instant;
import java.util.List;

public interface StockMovementRepositoryPort {
	StockMovement save(StockMovement movement);

	/** Movements recorded at or after {@code from}, the window stock levels are rebuilt from. */
	List<StockMovement> findByTimestampGreaterThanEqual(Instant from);
}
