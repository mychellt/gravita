package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBalanceRepositoryPort {

	Optional<StockBalance> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

	List<StockBalance> findByProductId(UUID productId);

	List<StockBalance> findByWarehouseId(UUID warehouseId);

	List<StockBalance> findAll();

	StockBalance save(StockBalance balance);

	/** The warehouse's balances, optionally of one product, ordered by product code. */
	Page<StockBalance> search(UUID warehouseId, UUID productId, PageQuery pageQuery);
}
