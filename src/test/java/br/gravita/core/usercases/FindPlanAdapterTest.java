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

import static br.gravita.core.domain.PlanFixtures.aPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@DisplayName("Returns the plan when it exists")
	@Test
	void shouldReturnPlanWhenFound() {
		FindPlanAdapter adapter = new FindPlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		PlanDomain plan = aPlan().id(id).build();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.of(plan));

		PlanDomain found = adapter.execute(new Context(id));

		assertThat(found).isEqualTo(plan);
	}

	@DisplayName("Fails with not found when the plan does not exist")
	@Test
	void shouldFailWhenPlanNotFound() {
		FindPlanAdapter adapter = new FindPlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(BusinessRuleException.class);
	}
}
