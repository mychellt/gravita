package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.shared.PageQuery;
import java.time.Instant;
import java.util.UUID;

/** {@code from} and {@code to} bound the movement timestamp, both inclusive and both optional. */
public record ListStockMovementsQuery(UUID productId, StockMovementType type, Instant from, Instant to,
		PageQuery pageQuery) {
}
