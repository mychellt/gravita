package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.LotJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotJpaRepository extends JpaRepository<LotJpaEntity, UUID> {

	Optional<LotJpaEntity> findByProductIdAndWarehouseIdAndCode(UUID productId, UUID warehouseId, String code);

	List<LotJpaEntity> findByExpiryDateLessThanEqual(LocalDate cutoffDate);

	List<LotJpaEntity> findByExpiryDateLessThanEqualAndWarehouseId(LocalDate cutoffDate, UUID warehouseId);

	@Query("""
			select l from LotJpaEntity l
			where l.warehouseId = :warehouseId
			and (:productId is null or l.productId = :productId)
			""")
	Page<LotJpaEntity> search(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId,
			Pageable pageable);
}
