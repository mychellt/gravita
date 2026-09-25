package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;

import java.util.Optional;

public interface StockTransferRepositoryPort {

	StockTransfer save(StockTransfer transfer);

	Optional<StockTransfer> findById(StockTransferId id);
}
