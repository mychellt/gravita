package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.StageTransitionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageTransitionJpaRepository extends JpaRepository<StageTransitionJpaEntity, UUID> {
	List<StageTransitionJpaEntity> findByOpportunityId(UUID opportunityId);
}
