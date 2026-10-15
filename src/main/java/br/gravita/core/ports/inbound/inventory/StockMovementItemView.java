package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockMovement;

public record StockMovementItemView(StockMovement movement, String productCode) {
}
