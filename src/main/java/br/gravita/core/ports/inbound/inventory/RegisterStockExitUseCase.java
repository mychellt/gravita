package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockMovement;

public interface RegisterStockExitUseCase {
	StockMovement execute(RegisterStockExitCommand command);
}
