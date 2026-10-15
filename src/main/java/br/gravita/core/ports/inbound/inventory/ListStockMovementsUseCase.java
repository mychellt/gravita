package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.Page;

public interface ListStockMovementsUseCase {
	/** Newest first. */
	Page<StockMovementItemView> execute(ListStockMovementsQuery query);
}
