package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesQuery;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesUseCase;
import br.gravita.core.ports.inbound.inventory.StockBalanceItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListStockBalancesService implements ListStockBalancesUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public ListStockBalancesService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final ProductRepositoryPort productRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public Page<StockBalanceItemView> execute(final ListStockBalancesQuery query) {
		final Page<StockBalance> balances = stockBalanceRepositoryPort.search(DefaultWarehouse.ID, query.productId(),
				query.pageQuery());
		final Map<UUID, ProductDomain> products = ProductCodeLookup.productsOf(productRepositoryPort,
				balances.content().stream().map(StockBalance::getProductId).toList());
		return new Page<>(
				balances.content().stream()
						.map(balance -> {
							final ProductDomain product = products.get(balance.getProductId());
							return StockBalanceItemView.from(balance, product == null ? null : product.getInternalCode(),
									product != null && Boolean.TRUE.equals(product.getAllowNegativeStock()));
						})
						.toList(),
				balances.page(), balances.size(), balances.totalElements());
	}
}
