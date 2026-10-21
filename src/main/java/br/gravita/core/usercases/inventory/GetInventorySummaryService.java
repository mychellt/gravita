package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.inbound.inventory.GetInventorySummaryUseCase;
import br.gravita.core.ports.inbound.inventory.InventorySummary;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A product is below its minimum when the stock available over all warehouses is less than that minimum (none counts as zero). */
@UseCase
public class GetInventorySummaryService implements GetInventorySummaryUseCase {

	private final ProductRepositoryPort productRepositoryPort;
	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;

	public GetInventorySummaryService(final ProductRepositoryPort productRepositoryPort,
			final StockBalanceRepositoryPort stockBalanceRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
	}

	@Override
	public InventorySummary execute() {
		final List<ProductDomain> active = productRepositoryPort.findAll().stream()
				.filter(product -> product.getStatus() == ProductStatus.ACTIVE).toList();
		final Map<UUID, BigDecimal> availableByProduct = new HashMap<>();
		for (final StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			availableByProduct.merge(balance.getProductId(), balance.available(), BigDecimal::add);
		}
		final int belowMinimum = (int) active.stream().filter(product -> {
			final StockParametersDomain stock = product.getStock();
			return stock != null && stock.minimum() != null
					&& availableByProduct.getOrDefault(product.getId(), BigDecimal.ZERO).compareTo(stock.minimum()) < 0;
		}).count();
		return new InventorySummary(active.size(), belowMinimum);
	}
}
