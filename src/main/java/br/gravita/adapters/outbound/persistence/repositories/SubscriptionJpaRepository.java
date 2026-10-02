package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
	boolean existsByPlanId(UUID planId);
}
