package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPlansAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@Test
	void shouldDelegateToRepositoryFindAll() {
		ListPlansAdapter adapter = new ListPlansAdapter(planRepositoryPort);
		PlanDomain plan = PlanDomain.builder().name("Bronze").tier(PlanTier.BRONZE)
				.priceMonthly(BigDecimal.ONE).priceAnnual(BigDecimal.ONE).features(List.of("x")).build();
		when(planRepositoryPort.findAll()).thenReturn(List.of(plan));

		List<PlanDomain> plans = adapter.execute(new Context());

		assertThat(plans).containsExactly(plan);
	}
}
