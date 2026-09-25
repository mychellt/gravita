package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockMovement;

public interface TransferStockUseCase {
	StockMovement initiate(InitiateTransferCommand command);

	StockMovement confirm(ConfirmTransferCommand command);
}
