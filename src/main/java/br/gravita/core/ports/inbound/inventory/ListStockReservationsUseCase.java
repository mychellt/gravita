package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.Page;

public interface ListStockReservationsUseCase {
	Page<StockReservationItemView> execute(ListStockReservationsQuery query);
}
