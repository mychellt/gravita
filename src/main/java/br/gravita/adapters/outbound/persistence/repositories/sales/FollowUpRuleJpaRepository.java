package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpRuleJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRuleJpaRepository extends JpaRepository<FollowUpRuleJpaEntity, UUID> {
	List<FollowUpRuleJpaEntity> findByActiveTrue();
}
