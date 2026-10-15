package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListLotsQuery;
import br.gravita.core.ports.inbound.inventory.ListLotsUseCase;
import br.gravita.core.ports.inbound.inventory.StockUnitView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListLotsService implements ListLotsUseCase {

	private final LotRepositoryPort lotRepositoryPort;
	private final SerialUnitRepositoryPort serialUnitRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public ListLotsService(final LotRepositoryPort lotRepositoryPort,
			final SerialUnitRepositoryPort serialUnitRepositoryPort, final ProductRepositoryPort productRepositoryPort) {
		this.lotRepositoryPort = lotRepositoryPort;
		this.serialUnitRepositoryPort = serialUnitRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public Page<StockUnitView> execute(final ListLotsQuery query) {
		return query.kind() == ListLotsQuery.Kind.SERIAL ? listSerialUnits(query) : listLots(query);
	}

	private Page<StockUnitView> listLots(final ListLotsQuery query) {
		final Page<Lot> lots = lotRepositoryPort.search(DefaultWarehouse.ID, query.productId(), query.pageQuery());
		final Map<UUID, String> codes = ProductCodeLookup.codesOf(productRepositoryPort,
				lots.content().stream().map(Lot::getProductId).toList());
		return new Page<>(
				lots.content().stream().map(lot -> StockUnitView.from(lot, codes.get(lot.getProductId()))).toList(),
				lots.page(), lots.size(), lots.totalElements());
	}

	private Page<StockUnitView> listSerialUnits(final ListLotsQuery query) {
		final Page<SerialUnit> units = serialUnitRepositoryPort.search(DefaultWarehouse.ID, query.productId(),
				query.pageQuery());
		final Map<UUID, String> codes = ProductCodeLookup.codesOf(productRepositoryPort,
				units.content().stream().map(SerialUnit::getProductId).toList());
		return new Page<>(
				units.content().stream().map(unit -> StockUnitView.from(unit, codes.get(unit.getProductId()))).toList(),
				units.page(), units.size(), units.totalElements());
	}
}
