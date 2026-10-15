package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.core.domain.inventory.PhysicalCountStatus;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhysicalCountJpaRepository extends JpaRepository<PhysicalCountJpaEntity, UUID> {

	@Query("""
			select c from PhysicalCountJpaEntity c
			where c.warehouseId = :warehouseId
			and (:status is null or c.status = :status)
			""")
	Page<PhysicalCountJpaEntity> search(@Param("warehouseId") UUID warehouseId,
			@Param("status") PhysicalCountStatus status, Pageable pageable);
}
