package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpTaskJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpTaskPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.FollowUpTaskJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpTaskRepositoryPort;
import java.time.LocalDate;
import java.util.UUID;

@PersistenceAdapter
class FollowUpTaskRepositoryAdapter implements FollowUpTaskRepositoryPort {

	private final FollowUpTaskJpaRepository jpaRepository;
	private final FollowUpTaskPersistenceMapper mapper;

	FollowUpTaskRepositoryAdapter(FollowUpTaskJpaRepository jpaRepository, FollowUpTaskPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public FollowUpTask save(FollowUpTask task) {
		FollowUpTaskJpaEntity entity = mapper.toEntity(task);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		FollowUpTaskJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public boolean existsForTargetOnDate(UUID opportunityId, UUID customerId, LocalDate dueDate) {
		if (opportunityId != null) {
			return jpaRepository.existsByOpportunityIdAndDueDate(opportunityId, dueDate);
		}
		return jpaRepository.existsByCustomerIdAndDueDate(customerId, dueDate);
	}
}
