package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.core.domain.inventory.StockReservationStatus;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {

	List<StockReservationJpaEntity> findByOrderRefAndStatus(UUID orderRef, StockReservationStatus status);

	@Query("""
			select r from StockReservationJpaEntity r
			where r.warehouseId = :warehouseId
			and (:status is null or r.status = :status)
			and (:productId is null or r.productId = :productId)
			""")
	Page<StockReservationJpaEntity> search(@Param("warehouseId") UUID warehouseId,
			@Param("status") StockReservationStatus status, @Param("productId") UUID productId, Pageable pageable);
}
