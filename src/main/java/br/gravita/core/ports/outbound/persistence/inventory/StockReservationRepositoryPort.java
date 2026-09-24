package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;

import java.util.Optional;

public interface StockReservationRepositoryPort {

	StockReservation save(StockReservation reservation);

	Optional<StockReservation> findById(StockReservationId id);
}
