package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpTaskJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpTaskPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.FollowUpTaskJpaRepository;
import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpTaskRepositoryAdapterTest {

	@Mock
	private FollowUpTaskJpaRepository repository;

	@Mock
	private FollowUpTaskPersistenceMapper mapper;

	@InjectMocks
	private FollowUpTaskRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new follow-up task marking its entity as new")
	void shouldSaveNewFollowUpTask() {
		final FollowUpTask task = buildTask(UUID.randomUUID(), null);
		final FollowUpTaskJpaEntity entity = FollowUpTaskJpaEntity.builder().id(task.getId().value()).build();
		final FollowUpTaskJpaEntity saved = FollowUpTaskJpaEntity.builder().id(task.getId().value()).build();
		when(mapper.map(task)).thenReturn(entity);
		when(repository.existsById(task.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(task);

		final FollowUpTask result = adapter.save(task);

		assertThat(result).isSameAs(task);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Checks existence by opportunity and due date when the task targets an opportunity")
	void existsForTargetOnDateChecksTheOpportunityWhenOneIsGiven() {
		final UUID opportunityId = UUID.randomUUID();
		final LocalDate dueDate = LocalDate.now();
		when(repository.existsByOpportunityIdAndDueDate(opportunityId, dueDate)).thenReturn(true);

		assertThat(adapter.existsForTargetOnDate(opportunityId, null, dueDate)).isTrue();
		verify(repository).existsByOpportunityIdAndDueDate(opportunityId, dueDate);
		verify(repository, never()).existsByCustomerIdAndDueDate(null, dueDate);
	}

	@Test
	@DisplayName("Checks existence by customer and due date when the task targets a customer")
	void existsForTargetOnDateChecksTheCustomerWhenNoOpportunityIsGiven() {
		final UUID customerId = UUID.randomUUID();
		final LocalDate dueDate = LocalDate.now();
		when(repository.existsByCustomerIdAndDueDate(customerId, dueDate)).thenReturn(false);

		assertThat(adapter.existsForTargetOnDate(null, customerId, dueDate)).isFalse();
		verify(repository).existsByCustomerIdAndDueDate(customerId, dueDate);
	}

	private FollowUpTask buildTask(final UUID opportunityId, final UUID customerId) {
		return FollowUpTask.of(FollowUpTaskId.of(UUID.randomUUID()), opportunityId, customerId, LocalDate.now(),
				UUID.randomUUID(), AlertChannel.APP);
	}
}
