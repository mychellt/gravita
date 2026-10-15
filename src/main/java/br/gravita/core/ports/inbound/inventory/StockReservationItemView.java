package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockReservation;

public record StockReservationItemView(StockReservation reservation, String productCode) {
}
