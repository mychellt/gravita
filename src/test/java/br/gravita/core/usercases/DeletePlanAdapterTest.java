package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.gravita.core.domain.PlanFixtures.plan;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@DisplayName("Deletes the plan when it exists and has no subscriptions")
	@Test
	void shouldDeleteWhenPlanExists() {
		final DeletePlanAdapter adapter = new DeletePlanAdapter(planRepositoryPort);
		final PlanDomain existing = plan().build();
		when(planRepositoryPort.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(planRepositoryPort.hasSubscriptions(existing.getId())).thenReturn(false);

		adapter.execute(new Context(existing.getId()));

		verify(planRepositoryPort).deleteById(existing.getId());
	}

	@DisplayName("Fails with not found when the plan to delete does not exist")
	@Test
	void shouldFailWhenPlanNotFound() {
		final DeletePlanAdapter adapter = new DeletePlanAdapter(planRepositoryPort);
		final UUID id = UUID.randomUUID();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).deleteById(id);
	}

	@DisplayName("Refuses to delete a plan that subscriptions still reference")
	@Test
	void shouldRejectDeletingPlanWithSubscriptions() {
		final DeletePlanAdapter adapter = new DeletePlanAdapter(planRepositoryPort);
		final PlanDomain existing = plan().build();
		when(planRepositoryPort.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(planRepositoryPort.hasSubscriptions(existing.getId())).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(existing.getId()))).isInstanceOf(BusinessRuleException.class);
		verify(planRepositoryPort, never()).deleteById(existing.getId());
	}
}
