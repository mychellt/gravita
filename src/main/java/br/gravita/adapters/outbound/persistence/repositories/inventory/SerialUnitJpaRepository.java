package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.SerialUnitJpaEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerialUnitJpaRepository extends JpaRepository<SerialUnitJpaEntity, UUID> {

	List<SerialUnitJpaEntity> findByProductIdAndWarehouseIdAndSerialNumberIn(UUID productId, UUID warehouseId,
			List<String> serialNumbers);

	@Query("""
			select u from SerialUnitJpaEntity u
			where u.warehouseId = :warehouseId
			and (:productId is null or u.productId = :productId)
			""")
	Page<SerialUnitJpaEntity> search(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId,
			Pageable pageable);
}
