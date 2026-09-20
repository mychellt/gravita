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
	private final PlanPersistenceMapper mapper = new PlanPersistenceMapper();

	PlanRepositoryAdapter(PlanJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public PlanDomain save(PlanDomain plan) {
		PlanJpaEntity saved = jpaRepository.save(mapper.toEntity(plan));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<PlanDomain> findById(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<PlanDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
