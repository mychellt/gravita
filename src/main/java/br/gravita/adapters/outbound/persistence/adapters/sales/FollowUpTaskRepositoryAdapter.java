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

	FollowUpTaskRepositoryAdapter(final FollowUpTaskJpaRepository jpaRepository, final FollowUpTaskPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public FollowUpTask save(final FollowUpTask task) {
		final FollowUpTaskJpaEntity entity = mapper.map(task);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final FollowUpTaskJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public boolean existsForTargetOnDate(final UUID opportunityId, final UUID customerId, final LocalDate dueDate) {
		if (opportunityId != null) {
			return jpaRepository.existsByOpportunityIdAndDueDate(opportunityId, dueDate);
		}
		return jpaRepository.existsByCustomerIdAndDueDate(customerId, dueDate);
	}
}
