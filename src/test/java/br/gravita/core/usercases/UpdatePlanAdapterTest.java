package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static br.gravita.core.domain.PlanFixtures.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	private UpdatePlanAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new UpdatePlanAdapter(planRepositoryPort);
		lenient().when(planRepositoryPort.save(any(PlanDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private PlanDomain givenStoredPlan(final PlanDomain... otherPlans) {
		final PlanDomain stored = plan().name("Silver").build();
		when(planRepositoryPort.findById(stored.getId())).thenReturn(Optional.of(stored));
		when(planRepositoryPort.findAll()).thenReturn(Stream.concat(Stream.of(stored), Stream.of(otherPlans)).toList());
		return stored;
	}

	@DisplayName("Saves the changes when the plan exists")
	@Test
	void shouldSaveWhenPlanExists() {
		final PlanDomain stored = givenStoredPlan();
		final PlanDomain updated = plan().id(stored.getId()).name("Silver Plus").build();

		final PlanDomain result = adapter.execute(new Context(updated));

		assertThat(result.getName()).isEqualTo("Silver Plus");
		verify(planRepositoryPort).save(updated);
	}

	@DisplayName("Fails with not found when the plan to update does not exist")
	@Test
	void shouldFailWhenPlanNotFound() {
		final PlanDomain updated = plan().build();
		when(planRepositoryPort.findById(updated.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(updated))).isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}

	@DisplayName("Keeping a plan's own name is not a duplicate")
	@Test
	void shouldAcceptUnchangedName() {
		final PlanDomain stored = givenStoredPlan();

		final PlanDomain result = adapter.execute(new Context(plan().id(stored.getId()).name("silver").build()));

		assertThat(result.getName()).isEqualTo("silver");
	}

	@DisplayName("Renaming a plan to another plan's name is rejected, ignoring case")
	@Test
	void shouldRejectRenameToExistingName() {
		final PlanDomain stored = givenStoredPlan(plan().name("Gold").build());

		assertThatThrownBy(() -> adapter.execute(new Context(plan().id(stored.getId()).name("GOLD").build())))
				.isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}

	@DisplayName("Featuring a plan clears the featured flag of the plan that had it")
	@Test
	void shouldClearFeaturedOnOtherPlans() {
		final PlanDomain previouslyFeatured = plan().name("Gold").featured(true).build();
		final PlanDomain stored = givenStoredPlan(previouslyFeatured);
		final PlanDomain updated = plan().id(stored.getId()).name("Silver").featured(true).build();

		adapter.execute(new Context(updated));

		assertThat(previouslyFeatured.isFeatured()).isFalse();
		verify(planRepositoryPort).save(previouslyFeatured);
		verify(planRepositoryPort).save(updated);
	}

	@DisplayName("Saving a plan that stays featured does not rewrite the other plans")
	@Test
	void shouldNotRewriteOtherPlansWhenNothingToClear() {
		final PlanDomain stored = givenStoredPlan(plan().name("Gold").build());

		adapter.execute(new Context(plan().id(stored.getId()).name("Silver").featured(true).build()));

		verify(planRepositoryPort, times(1)).save(any());
	}

	@DisplayName("Deactivating the only active plan is rejected")
	@Test
	void shouldRejectDeactivatingTheLastActivePlan() {
		final PlanDomain stored = givenStoredPlan(plan().name("Gold").active(false).build());

		assertThatThrownBy(() -> adapter.execute(new Context(plan().id(stored.getId()).name("Silver").active(false).build())))
				.isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}

	@DisplayName("Deactivating a plan is allowed while another plan stays active")
	@Test
	void shouldAllowDeactivatingWhenAnotherPlanIsActive() {
		final PlanDomain stored = givenStoredPlan(plan().name("Gold").build());

		final PlanDomain result = adapter.execute(new Context(plan().id(stored.getId()).name("Silver").active(false).build()));

		assertThat(result.isActivePlan()).isFalse();
	}

	@ParameterizedTest(name = "rejects {0}")
	@MethodSource("br.gravita.core.domain.PlanFixtures#invalidPlans")
	void shouldRejectInvalidPlan(final String ignoredRule, final PlanDomain invalid) {
		when(planRepositoryPort.findById(invalid.getId())).thenReturn(Optional.of(plan().id(invalid.getId()).build()));

		assertThatThrownBy(() -> adapter.execute(new Context(invalid))).isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}
}
