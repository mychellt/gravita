package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.InteractionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionJpaRepository extends JpaRepository<InteractionJpaEntity, UUID> {
	List<InteractionJpaEntity> findByOpportunityId(UUID opportunityId);

	List<InteractionJpaEntity> findByCustomerId(UUID customerId);
}
