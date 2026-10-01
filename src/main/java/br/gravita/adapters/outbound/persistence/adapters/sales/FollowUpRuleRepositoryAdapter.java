package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpRulePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.FollowUpRuleJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class FollowUpRuleRepositoryAdapter implements FollowUpRuleRepositoryPort {

	private final FollowUpRuleJpaRepository jpaRepository;
	private final FollowUpRulePersistenceMapper mapper;

	FollowUpRuleRepositoryAdapter(FollowUpRuleJpaRepository jpaRepository, FollowUpRulePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public FollowUpRule save(FollowUpRule rule) {
		FollowUpRuleJpaEntity entity = mapper.map(rule);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		FollowUpRuleJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<FollowUpRule> findById(FollowUpRuleId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<FollowUpRule> findAll() {
		return jpaRepository.findAll().stream().map(mapper::map).toList();
	}

	@Override
	public List<FollowUpRule> findAllActive() {
		return jpaRepository.findByActiveTrue().stream().map(mapper::map).toList();
	}

	@Override
	public void deleteById(FollowUpRuleId id) {
		jpaRepository.deleteById(id.value());
	}
}
