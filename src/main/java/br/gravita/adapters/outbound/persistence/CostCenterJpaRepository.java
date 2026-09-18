package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.CostCenterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CostCenterJpaRepository extends JpaRepository<CostCenterJpaEntity, UUID> {
	boolean existsByParentId(UUID parentId);
}
