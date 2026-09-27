package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.OpportunityJpaEntity;
import br.gravita.core.domain.sales.OpportunityStage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityJpaRepository extends JpaRepository<OpportunityJpaEntity, UUID> {
	List<OpportunityJpaEntity> findByStage(OpportunityStage stage);
}
