package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepositoryPort {

	StockReservation save(StockReservation reservation);

	Optional<StockReservation> findById(StockReservationId id);

	List<StockReservation> findActiveByOrderRef(UUID orderRef);

	/** The warehouse's reservations matching the query's filters, newest first. */
	Page<StockReservation> search(UUID warehouseId, ListStockReservationsQuery query);
}
