package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.Page;

public interface ListLotsUseCase {
	Page<StockUnitView> execute(ListLotsQuery query);
}
