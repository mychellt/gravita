package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.domain.shared.PageQuery;
import java.util.UUID;

public record ListStockReservationsQuery(StockReservationStatus status, UUID productId, PageQuery pageQuery) {
}
