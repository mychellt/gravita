package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.ProductDomain;

public interface SetAllowNegativeStockUseCase {
	ProductDomain execute(SetAllowNegativeStockCommand command);
}
