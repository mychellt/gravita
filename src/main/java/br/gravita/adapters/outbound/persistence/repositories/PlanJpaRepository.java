package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.core.domain.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {
	Optional<PlanJpaEntity> findFirstByTierAndActiveTrueOrderByCreatedAt(PlanTier tier);
}
