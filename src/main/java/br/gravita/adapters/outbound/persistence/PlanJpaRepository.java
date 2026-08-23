package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {
}
