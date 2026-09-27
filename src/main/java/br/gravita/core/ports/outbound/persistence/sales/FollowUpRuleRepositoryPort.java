package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import java.util.List;
import java.util.Optional;

public interface FollowUpRuleRepositoryPort {
	FollowUpRule save(FollowUpRule rule);

	Optional<FollowUpRule> findById(FollowUpRuleId id);

	List<FollowUpRule> findAll();

	List<FollowUpRule> findAllActive();

	void deleteById(FollowUpRuleId id);
}
