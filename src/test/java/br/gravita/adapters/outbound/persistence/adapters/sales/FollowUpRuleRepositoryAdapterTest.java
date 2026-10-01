package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpRulePersistenceMapperImpl;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({FollowUpRuleRepositoryAdapter.class, FollowUpRulePersistenceMapperImpl.class})
class FollowUpRuleRepositoryAdapterTest {

	@Autowired
	private FollowUpRuleRepositoryAdapter repositoryAdapter;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("Persists a follow-up rule and reloads it intact")
	void shouldPersistAndReloadAFollowUpRule() {
		FollowUpRule rule =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 7, FollowUpTarget.CUSTOMER, true, true);

		FollowUpRule saved = repositoryAdapter.save(rule);

		assertThat(repositoryAdapter.findById(saved.getId())).isPresent().get().satisfies(found -> {
			assertThat(found.getDaysWithoutContact()).isEqualTo(7);
			assertThat(found.getTarget()).isEqualTo(FollowUpTarget.CUSTOMER);
			assertThat(found.isNotifyOwner()).isTrue();
			assertThat(found.isActive()).isTrue();
		});
	}

	@Test
	@DisplayName("Excludes inactive rules when finding all active rules")
	void findAllActiveExcludesInactiveRules() {
		FollowUpRule active =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 7, FollowUpTarget.CUSTOMER, true, true);
		FollowUpRule inactive =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 14, FollowUpTarget.OPPORTUNITY, false, false);
		repositoryAdapter.save(active);
		repositoryAdapter.save(inactive);

		assertThat(repositoryAdapter.findAllActive())
				.extracting(rule -> rule.getId().value())
				.containsExactly(active.getId().value());
	}

	@Test
	@DisplayName("Removes the rule when deleting by id")
	void deleteByIdRemovesTheRule() {
		FollowUpRule rule =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 7, FollowUpTarget.CUSTOMER, true, true);
		FollowUpRule saved = repositoryAdapter.save(rule);
		entityManager.flush();
		entityManager.clear();

		repositoryAdapter.deleteById(saved.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(repositoryAdapter.findById(saved.getId())).isEmpty();
	}
}
