package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.ports.persistence.PlanRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePlanAdapterTest {

	@Mock
	private PlanRepositoryPort planRepositoryPort;

	@Test
	void shouldAssignIdAndSaveNewPlan() {
		CreatePlanAdapter adapter = new CreatePlanAdapter(planRepositoryPort);
		PlanDomain plan = PlanDomain.builder()
				.name("Bronze")
				.tier(PlanTier.BRONZE)
				.priceMonthly(new BigDecimal("297"))
				.priceAnnual(new BigDecimal("247"))
				.features(List.of("1 CNPJ · 1 filial"))
				.build();
		when(planRepositoryPort.save(any(PlanDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PlanDomain created = adapter.execute(new Context(plan));

		assertThat(created.getId()).isNotNull();
		ArgumentCaptor<PlanDomain> captor = ArgumentCaptor.forClass(PlanDomain.class);
		verify(planRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(created.getId());
		assertThat(captor.getValue().getName()).isEqualTo("Bronze");
	}

	@Test
	void shouldKeepExistingIdWhenAlreadySet() {
		CreatePlanAdapter adapter = new CreatePlanAdapter(planRepositoryPort);
		UUID existingId = UUID.randomUUID();
		PlanDomain plan = PlanDomain.builder().id(existingId).name("Gold").tier(PlanTier.GOLD)
				.priceMonthly(BigDecimal.ONE).priceAnnual(BigDecimal.ONE).features(List.of("x")).build();
		when(planRepositoryPort.save(any(PlanDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PlanDomain created = adapter.execute(new Context(plan));

		assertThat(created.getId()).isEqualTo(existingId);
	}
}
