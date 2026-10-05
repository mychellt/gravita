package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import br.gravita.core.ports.outbound.inventory.NotifyLowStockPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;

import java.util.List;
import java.util.Optional;

@UseCase
public class SuggestReorderService implements SuggestReorderUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final NotifyLowStockPort notifyLowStockPort;

	public SuggestReorderService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final ProductRepositoryPort productRepositoryPort, final NotifyLowStockPort notifyLowStockPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
		this.notifyLowStockPort = notifyLowStockPort;
	}

	@Override
	public List<ReorderSuggestion> execute(final SuggestReorderQuery query) {
		final List<StockBalance> balances = query.warehouseId() != null
				? stockBalanceRepositoryPort.findByWarehouseId(query.warehouseId())
				: stockBalanceRepositoryPort.findAll();

		final List<ReorderSuggestion> suggestions = balances.stream().map(this::toSuggestion).flatMap(Optional::stream)
				.toList();
		notifyLowStockPort.notify(suggestions);
		return suggestions;
	}

	private Optional<ReorderSuggestion> toSuggestion(final StockBalance balance) {
		return productRepositoryPort.get(balance.getProductId())
				.map(ProductDomain::getStock)
				.filter(stock -> stock != null && stock.reorderPoint() != null)
				.filter(stock -> stock.isAtOrBelowReorderPoint(balance.available()))
				.map(stock -> ReorderSuggestion.of(balance, stock));
	}
}
