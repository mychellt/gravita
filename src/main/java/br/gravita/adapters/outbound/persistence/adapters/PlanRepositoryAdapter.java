package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.PlanPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.PlanJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.SubscriptionJpaRepository;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PlanRepositoryAdapter implements PlanRepositoryPort {

	private final PlanJpaRepository jpaRepository;
	private final SubscriptionJpaRepository subscriptionJpaRepository;
	private final PlanPersistenceMapper mapper;

	PlanRepositoryAdapter(PlanJpaRepository jpaRepository, SubscriptionJpaRepository subscriptionJpaRepository,
			PlanPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.subscriptionJpaRepository = subscriptionJpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PlanDomain save(PlanDomain plan) {
		PlanJpaEntity entity = mapper.map(plan);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PlanJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PlanDomain> findById(UUID id) {
		return jpaRepository.findById(id).map(mapper::map);
	}

	@Override
	public List<PlanDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::map).toList();
	}

	@Override
	public Optional<PlanDomain> findActiveByTier(PlanTier tier) {
		return jpaRepository.findFirstByTierAndActiveTrueOrderByCreatedAt(tier).map(mapper::map);
	}

	@Override
	public boolean hasSubscriptions(UUID id) {
		return subscriptionJpaRepository.existsByPlanId(id);
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.findById(id).ifPresent(entity -> {
			entity.setNew(false);
			jpaRepository.delete(entity);
		});
	}
}
