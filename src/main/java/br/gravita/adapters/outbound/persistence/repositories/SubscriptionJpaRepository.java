package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
	boolean existsByPlanId(UUID planId);

	List<SubscriptionJpaEntity> findAllByOrderByActivationDateDesc();

	List<SubscriptionJpaEntity> findAllByCompanyId(UUID companyId);
}
