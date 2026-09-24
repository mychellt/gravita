package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceQuery;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceUseCase;
import br.gravita.core.ports.inbound.inventory.StockBalanceView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;

@UseCase
public class GetStockBalanceService implements GetStockBalanceUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public GetStockBalanceService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			ProductRepositoryPort productRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public StockBalanceView execute(GetStockBalanceQuery query) {
		productRepositoryPort.get(query.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + query.productId()));

		if (query.warehouseId() != null) {
			return stockBalanceRepositoryPort.findByProductIdAndWarehouseId(query.productId(), query.warehouseId())
					.map(balance -> StockBalanceView.from(query.productId(), query.warehouseId(), balance))
					.orElseGet(() -> StockBalanceView.zero(query.productId(), query.warehouseId()));
		}

		return StockBalanceView.aggregate(query.productId(),
				stockBalanceRepositoryPort.findByProductId(query.productId()));
	}
}
