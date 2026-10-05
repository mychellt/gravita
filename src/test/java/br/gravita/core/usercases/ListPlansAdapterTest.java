package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.gravita.core.domain.PlanFixtures.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPlansAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@DisplayName("Lists plans by delegating to the repository's find-all")
	@Test
	void shouldDelegateToRepositoryFindAll() {
		final ListPlansAdapter adapter = new ListPlansAdapter(planRepositoryPort);
		final PlanDomain plan = plan().build();
		when(planRepositoryPort.findAll()).thenReturn(List.of(plan));

		final List<PlanDomain> plans = adapter.execute(new Context());

		assertThat(plans).containsExactly(plan);
	}

	@DisplayName("Reads inside a transaction so the plan's lazy features can be mapped (open-in-view is off)")
	@Test
	void shouldReadInsideATransaction() throws NoSuchMethodException {
		final Transactional transactional = ListPlansAdapter.class.getMethod("execute", Context.class).getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isTrue();
	}
}
