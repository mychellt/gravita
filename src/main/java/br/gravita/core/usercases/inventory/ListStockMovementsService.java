package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsUseCase;
import br.gravita.core.ports.inbound.inventory.StockMovementItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListStockMovementsService implements ListStockMovementsUseCase {

	private final StockMovementRepositoryPort stockMovementRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public ListStockMovementsService(final StockMovementRepositoryPort stockMovementRepositoryPort,
			final ProductRepositoryPort productRepositoryPort) {
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public Page<StockMovementItemView> execute(final ListStockMovementsQuery query) {
		final Page<StockMovement> movements = stockMovementRepositoryPort.search(DefaultWarehouse.ID, query);
		final Map<UUID, String> codes = ProductCodeLookup.codesOf(productRepositoryPort,
				movements.content().stream().map(StockMovement::getProductId).toList());
		return new Page<>(
				movements.content().stream()
						.map(movement -> new StockMovementItemView(movement, codes.get(movement.getProductId())))
						.toList(),
				movements.page(), movements.size(), movements.totalElements());
	}
}
