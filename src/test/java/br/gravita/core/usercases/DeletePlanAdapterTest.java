package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@DisplayName("Deletes the plan when it exists")
	@Test
	void shouldDeleteWhenPlanExists() {
		DeletePlanAdapter adapter = new DeletePlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		PlanDomain existing = PlanDomain.builder().id(id).name("Gold").tier(PlanTier.GOLD)
				.priceMonthly(BigDecimal.TEN).priceAnnual(BigDecimal.ONE).features(List.of("x")).build();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.of(existing));

		adapter.execute(new Context(id));

		verify(planRepositoryPort).deleteById(id);
	}

	@DisplayName("Fails with not found when the plan to delete does not exist")
	@Test
	void shouldFailWhenPlanNotFound() {
		DeletePlanAdapter adapter = new DeletePlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(BusinessRuleException.class);
	}
}
