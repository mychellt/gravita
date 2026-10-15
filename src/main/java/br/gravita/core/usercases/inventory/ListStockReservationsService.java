package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsUseCase;
import br.gravita.core.ports.inbound.inventory.StockReservationItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListStockReservationsService implements ListStockReservationsUseCase {

	private final StockReservationRepositoryPort stockReservationRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public ListStockReservationsService(final StockReservationRepositoryPort stockReservationRepositoryPort,
			final ProductRepositoryPort productRepositoryPort) {
		this.stockReservationRepositoryPort = stockReservationRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public Page<StockReservationItemView> execute(final ListStockReservationsQuery query) {
		final Page<StockReservation> reservations = stockReservationRepositoryPort.search(DefaultWarehouse.ID, query);
		final Map<UUID, String> codes = ProductCodeLookup.codesOf(productRepositoryPort,
				reservations.content().stream().map(StockReservation::getProductId).toList());
		return new Page<>(
				reservations.content().stream()
						.map(reservation -> new StockReservationItemView(reservation,
								codes.get(reservation.getProductId())))
						.toList(),
				reservations.page(), reservations.size(), reservations.totalElements());
	}
}
