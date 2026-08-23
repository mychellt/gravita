package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@Test
	void shouldSaveWhenPlanExists() {
		UpdatePlanAdapter adapter = new UpdatePlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		PlanDomain existing = PlanDomain.builder().id(id).name("Silver").tier(PlanTier.SILVER)
				.priceMonthly(BigDecimal.TEN).priceAnnual(BigDecimal.ONE).features(List.of("x")).build();
		PlanDomain updated = PlanDomain.builder().id(id).name("Silver Plus").tier(PlanTier.SILVER)
				.priceMonthly(BigDecimal.TEN).priceAnnual(BigDecimal.ONE).features(List.of("x", "y")).build();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(planRepositoryPort.save(updated)).thenReturn(updated);

		PlanDomain result = adapter.execute(new Context(updated));

		assertThat(result.getName()).isEqualTo("Silver Plus");
	}

	@Test
	void shouldFailWhenPlanNotFound() {
		UpdatePlanAdapter adapter = new UpdatePlanAdapter(planRepositoryPort);
		UUID id = UUID.randomUUID();
		PlanDomain updated = PlanDomain.builder().id(id).name("Silver Plus").tier(PlanTier.SILVER)
				.priceMonthly(BigDecimal.TEN).priceAnnual(BigDecimal.ONE).features(List.of("x")).build();
		when(planRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(updated))).isInstanceOf(BusinessRuleException.class);
	}
}
