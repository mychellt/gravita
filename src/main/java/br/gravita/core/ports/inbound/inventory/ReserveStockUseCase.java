package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockReservation;

public interface ReserveStockUseCase {
	StockReservation execute(ReserveStockCommand command);
}
