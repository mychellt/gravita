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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

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
class CreatePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	private CreatePlanAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new CreatePlanAdapter(planRepositoryPort);
		lenient().when(planRepositoryPort.save(any(PlanDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@DisplayName("Creating a plan assigns a new id and saves every field")
	@Test
	void shouldAssignIdAndSaveNewPlan() {
		final PlanDomain plan = plan().id(null).build();

		final PlanDomain created = adapter.execute(new Context(plan));

		assertThat(created.getId()).isNotNull();
		final ArgumentCaptor<PlanDomain> captor = ArgumentCaptor.forClass(PlanDomain.class);
		verify(planRepositoryPort).save(captor.capture());
		final PlanDomain saved = captor.getValue();
		assertThat(saved.getId()).isEqualTo(created.getId());
		assertThat(saved.getName()).isEqualTo("Bronze");
		assertThat(saved.getDescription()).isEqualTo("Para quem está começando");
		assertThat(saved.isActivePlan()).isTrue();
		assertThat(saved.getLimits().usuarios()).isEqualTo(3);
		assertThat(saved.getFeatures()).hasSize(2);
		assertThat(saved.getSupport().slaHoras()).isEqualTo(24);
	}

	@DisplayName("Creating a plan that already has an id keeps that id")
	@Test
	void shouldKeepExistingIdWhenAlreadySet() {
		final UUID existingId = UUID.randomUUID();

		final PlanDomain created = adapter.execute(new Context(plan().id(existingId).build()));

		assertThat(created.getId()).isEqualTo(existingId);
	}

	@DisplayName("Featuring a new plan clears the featured flag of the plan that had it")
	@Test
	void shouldClearFeaturedOnOtherPlans() {
		final PlanDomain previouslyFeatured = plan().name("Silver").featured(true).build();
		final PlanDomain notFeatured = plan().name("Gold").build();
		when(planRepositoryPort.findAll()).thenReturn(List.of(previouslyFeatured, notFeatured));

		adapter.execute(new Context(plan().id(null).featured(true).build()));

		assertThat(previouslyFeatured.isFeatured()).isFalse();
		verify(planRepositoryPort).save(previouslyFeatured);
		verify(planRepositoryPort, never()).save(notFeatured);
	}

	@DisplayName("Creating a plan that is not featured leaves the other plans untouched")
	@Test
	void shouldNotTouchOtherPlansWhenNotFeatured() {
		final PlanDomain featured = plan().name("Silver").featured(true).build();
		when(planRepositoryPort.findAll()).thenReturn(List.of(featured));

		adapter.execute(new Context(plan().id(null).build()));

		assertThat(featured.isFeatured()).isTrue();
		verify(planRepositoryPort, times(1)).save(any());
	}

	@DisplayName("Rejects a name that another plan already uses, ignoring case and surrounding spaces")
	@Test
	void shouldRejectDuplicateNameIgnoringCase() {
		when(planRepositoryPort.findAll()).thenReturn(List.of(plan().name("Bronze").build()));

		assertThatThrownBy(() -> adapter.execute(new Context(plan().id(null).name("  bRoNzE ").build())))
				.isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}

	@DisplayName("Rejects an inactive plan when no other plan is active")
	@Test
	void shouldRejectWhenNoPlanWouldBeActive() {
		when(planRepositoryPort.findAll()).thenReturn(List.of(plan().name("Silver").active(false).build()));

		assertThatThrownBy(() -> adapter.execute(new Context(plan().id(null).active(false).build())))
				.isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}

	@DisplayName("Accepts an inactive plan while another plan is active")
	@Test
	void shouldAcceptInactivePlanWhenAnotherIsActive() {
		when(planRepositoryPort.findAll()).thenReturn(List.of(plan().name("Silver").build()));

		final PlanDomain created = adapter.execute(new Context(plan().id(null).active(false).build()));

		assertThat(created.isActivePlan()).isFalse();
	}

	@ParameterizedTest(name = "rejects {0}")
	@MethodSource("br.gravita.core.domain.PlanFixtures#invalidPlans")
	void shouldRejectInvalidPlan(final String ignoredRule, final PlanDomain plan) {
		assertThatThrownBy(() -> adapter.execute(new Context(plan))).isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).save(any());
	}
}
