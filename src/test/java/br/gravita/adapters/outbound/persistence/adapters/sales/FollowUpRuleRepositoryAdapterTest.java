package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpRulePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.FollowUpRuleJpaRepository;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpRuleRepositoryAdapterTest {

	@Mock
	private FollowUpRuleJpaRepository repository;

	@Mock
	private FollowUpRulePersistenceMapper mapper;

	@InjectMocks
	private FollowUpRuleRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new follow-up rule marking its entity as new")
	void shouldSaveNewFollowUpRule() {
		final FollowUpRule rule = buildRule(7);
		final FollowUpRuleJpaEntity entity = buildEntity(rule.getId());
		final FollowUpRuleJpaEntity saved = buildEntity(rule.getId());
		when(mapper.map(rule)).thenReturn(entity);
		when(repository.existsById(rule.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(rule);

		final FollowUpRule result = adapter.save(rule);

		assertThat(result).isSameAs(rule);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing follow-up rule marking its entity as not new")
	void shouldSaveExistingFollowUpRuleAsNotNew() {
		final FollowUpRule rule = buildRule(7);
		final FollowUpRuleJpaEntity entity = buildEntity(rule.getId());
		when(mapper.map(rule)).thenReturn(entity);
		when(repository.existsById(rule.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(rule);

		adapter.save(rule);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a follow-up rule by id")
	void shouldFindFollowUpRuleById() {
		final FollowUpRule rule = buildRule(7);
		final FollowUpRuleJpaEntity entity = buildEntity(rule.getId());
		when(repository.findById(rule.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(rule);

		final Optional<FollowUpRule> result = adapter.findById(rule.getId());

		assertThat(result).contains(rule);
		verify(repository).findById(rule.getId().value());
	}

	@Test
	@DisplayName("Lists all follow-up rules")
	void shouldListAllFollowUpRules() {
		final FollowUpRule first = buildRule(7);
		final FollowUpRule second = buildRule(14);
		final FollowUpRuleJpaEntity firstEntity = buildEntity(first.getId());
		final FollowUpRuleJpaEntity secondEntity = buildEntity(second.getId());
		when(repository.findAll()).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<FollowUpRule> result = adapter.findAll();

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Lists only the active follow-up rules")
	void shouldListOnlyActiveFollowUpRules() {
		final FollowUpRule active = buildRule(7);
		final FollowUpRuleJpaEntity activeEntity = buildEntity(active.getId());
		when(repository.findByActiveTrue()).thenReturn(List.of(activeEntity));
		when(mapper.map(activeEntity)).thenReturn(active);

		final List<FollowUpRule> result = adapter.findAllActive();

		assertThat(result).containsExactly(active);
		verify(repository).findByActiveTrue();
	}

	@Test
	@DisplayName("Deletes the rule by id")
	void shouldDeleteRuleById() {
		final FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());

		adapter.deleteById(id);

		verify(repository).deleteById(id.value());
	}

	private FollowUpRuleJpaEntity buildEntity(final FollowUpRuleId id) {
		return FollowUpRuleJpaEntity.builder().id(id.value()).build();
	}

	private FollowUpRule buildRule(final int daysWithoutContact) {
		return FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), daysWithoutContact, FollowUpTarget.CUSTOMER, true,
				true);
	}
}
