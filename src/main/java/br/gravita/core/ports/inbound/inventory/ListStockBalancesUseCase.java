package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.Page;

public interface ListStockBalancesUseCase {
	Page<StockBalanceItemView> execute(ListStockBalancesQuery query);
}
