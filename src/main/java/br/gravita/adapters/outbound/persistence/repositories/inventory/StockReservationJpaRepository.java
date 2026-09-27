package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.core.domain.inventory.StockReservationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {

	List<StockReservationJpaEntity> findByOrderRefAndStatus(UUID orderRef, StockReservationStatus status);
}
