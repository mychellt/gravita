package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.PlanPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.PlanJpaRepository;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PlanRepositoryAdapter implements PlanRepositoryPort {

	private final PlanJpaRepository jpaRepository;
	private final PlanPersistenceMapper mapper;

	PlanRepositoryAdapter(PlanJpaRepository jpaRepository, PlanPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PlanDomain save(PlanDomain plan) {
		PlanJpaEntity saved = jpaRepository.save(mapper.map(plan));
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
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
