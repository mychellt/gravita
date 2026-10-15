package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockMovementJpaEntity;
import br.gravita.core.domain.inventory.StockMovementType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, UUID> {

	List<StockMovementJpaEntity> findByTimestampGreaterThanEqual(Instant from);

	@Query("""
			select m from StockMovementJpaEntity m
			where m.warehouseId = :warehouseId
			and (:productId is null or m.productId = :productId)
			and (:type is null or m.type = :type)
			and (cast(:from as Instant) is null or m.timestamp >= :from)
			and (cast(:to as Instant) is null or m.timestamp <= :to)
			""")
	Page<StockMovementJpaEntity> search(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId,
			@Param("type") StockMovementType type, @Param("from") Instant from, @Param("to") Instant to,
			Pageable pageable);
}
