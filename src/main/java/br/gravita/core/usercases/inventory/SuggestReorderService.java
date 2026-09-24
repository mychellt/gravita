package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;

import java.util.List;
import java.util.Optional;

@UseCase
public class SuggestReorderService implements SuggestReorderUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public SuggestReorderService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			ProductRepositoryPort productRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public List<ReorderSuggestion> execute(SuggestReorderQuery query) {
		List<StockBalance> balances = query.warehouseId() != null
				? stockBalanceRepositoryPort.findByWarehouseId(query.warehouseId())
				: stockBalanceRepositoryPort.findAll();

		return balances.stream().map(this::toSuggestion).flatMap(Optional::stream).toList();
	}

	/**
	 * A product with no configured reorder point (e.g. not yet parameterized
	 * in masterdata) is skipped rather than treated as always/never due for
	 * reorder.
	 */
	private Optional<ReorderSuggestion> toSuggestion(StockBalance balance) {
		return productRepositoryPort.get(balance.getProductId())
				.map(ProductDomain::getStock)
				.filter(stock -> stock != null && stock.reorderPoint() != null)
				.filter(stock -> stock.isAtOrBelowReorderPoint(balance.available()))
				.map(stock -> ReorderSuggestion.of(balance, stock));
	}
}
