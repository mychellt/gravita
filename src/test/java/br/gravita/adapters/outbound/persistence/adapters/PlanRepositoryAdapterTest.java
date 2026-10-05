package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.PlanPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.PlanJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.SubscriptionJpaRepository;
import br.gravita.core.domain.PlanDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.gravita.core.domain.PlanFixtures.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanRepositoryAdapterTest {

	@Mock
	private PlanJpaRepository repository;

	@Mock
	private SubscriptionJpaRepository subscriptionRepository;

	@Mock
	private PlanPersistenceMapper mapper;

	@InjectMocks
	private PlanRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new plan marking its entity as new")
	void shouldSaveNewPlan() {
		final PlanDomain plan = buildPlan("Bronze");
		final PlanJpaEntity entity = buildEntity(plan.getId());
		final PlanJpaEntity saved = buildEntity(plan.getId());
		when(mapper.map(plan)).thenReturn(entity);
		when(repository.existsById(plan.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(plan);

		final PlanDomain result = adapter.save(plan);

		assertThat(result).isSameAs(plan);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing plan marking its entity as not new")
	void shouldSaveExistingPlanAsNotNew() {
		final PlanDomain plan = buildPlan("Bronze");
		final PlanJpaEntity entity = buildEntity(plan.getId());
		when(mapper.map(plan)).thenReturn(entity);
		when(repository.existsById(plan.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(plan);

		adapter.save(plan);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a plan by id")
	void shouldFindPlanById() {
		final PlanDomain plan = buildPlan("Bronze");
		final PlanJpaEntity entity = buildEntity(plan.getId());
		when(repository.findById(plan.getId())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(plan);

		final Optional<PlanDomain> result = adapter.findById(plan.getId());

		assertThat(result).contains(plan);
		verify(repository).findById(plan.getId());
	}

	@Test
	@DisplayName("Lists all saved plans")
	void shouldListAllPlans() {
		final PlanDomain silver = buildPlan("Silver");
		final PlanDomain gold = buildPlan("Gold");
		final PlanJpaEntity silverEntity = buildEntity(silver.getId());
		final PlanJpaEntity goldEntity = buildEntity(gold.getId());
		when(repository.findAll()).thenReturn(List.of(silverEntity, goldEntity));
		when(mapper.map(same(silverEntity))).thenReturn(silver);
		when(mapper.map(same(goldEntity))).thenReturn(gold);

		final List<PlanDomain> result = adapter.findAll();

		assertThat(result).containsExactly(silver, gold);
	}

	@Test
	@DisplayName("Deletes a saved plan")
	void shouldDeletePlan() {
		final UUID id = UUID.randomUUID();
		final PlanJpaEntity entity = buildEntity(id);
		when(repository.findById(id)).thenReturn(Optional.of(entity));

		adapter.deleteById(id);

		assertThat(entity.isNew()).isFalse();
		verify(repository).delete(entity);
	}

	@Test
	@DisplayName("Does nothing when deleting a plan that does not exist")
	void shouldDoNothingWhenDeletingUnknownPlan() {
		final UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		adapter.deleteById(id);

		verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("Reports whether any subscription references the plan")
	void shouldReportSubscriptionsReferencingPlan() {
		final UUID referenced = UUID.randomUUID();
		final UUID unreferenced = UUID.randomUUID();
		when(subscriptionRepository.existsByPlanId(referenced)).thenReturn(true);
		when(subscriptionRepository.existsByPlanId(unreferenced)).thenReturn(false);

		assertThat(adapter.hasSubscriptions(referenced)).isTrue();
		assertThat(adapter.hasSubscriptions(unreferenced)).isFalse();
	}

	private PlanJpaEntity buildEntity(final UUID id) {
		return PlanJpaEntity.builder().id(id).build();
	}

	private PlanDomain buildPlan(final String name) {
		return plan().name(name).build();
	}
}
