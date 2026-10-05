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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static br.gravita.core.domain.PlanFixtures.plan;
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
		final FindPlanAdapter adapter = new FindPlanAdapter(planRepositoryPort);
		final UUID id = UUID.randomUUID();
		final PlanDomain plan = plan().id(id).build();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.of(plan));

		final PlanDomain found = adapter.execute(new Context(id));

		assertThat(found).isEqualTo(plan);
	}

	@DisplayName("Fails with not found when the plan does not exist")
	@Test
	void shouldFailWhenPlanNotFound() {
		final FindPlanAdapter adapter = new FindPlanAdapter(planRepositoryPort);
		final UUID id = UUID.randomUUID();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Reads inside a transaction so the plan's lazy features can be mapped (open-in-view is off)")
	@Test
	void shouldReadInsideATransaction() throws NoSuchMethodException {
		final Transactional transactional = FindPlanAdapter.class.getMethod("execute", Context.class).getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isTrue();
	}
}
