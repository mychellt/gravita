package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.PlanPersistenceMapperImpl;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PlanRepositoryAdapter.class, PlanPersistenceMapperImpl.class})
class PlanRepositoryAdapterTest {

	@Autowired
	private PlanRepositoryAdapter repositoryAdapter;

	@Test
	@DisplayName("Saves a plan and retrieves it by id")
	void shouldSaveAndRetrievePlan() {
		PlanDomain plan = PlanDomain.builder()
				.name("Bronze")
				.tier(PlanTier.BRONZE)
				.priceMonthly(new BigDecimal("297.00"))
				.priceAnnual(new BigDecimal("247.00"))
				.features(List.of("1 CNPJ · 1 filial", "NF-e e NFC-e ilimitadas"))
				.build();
		plan.setId(UUID.randomUUID());

		PlanDomain saved = repositoryAdapter.save(plan);

		assertThat(repositoryAdapter.findById(saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getName()).isEqualTo("Bronze");
					assertThat(found.getTier()).isEqualTo(PlanTier.BRONZE);
					assertThat(found.getFeatures()).containsExactlyInAnyOrder("1 CNPJ · 1 filial", "NF-e e NFC-e ilimitadas");
				});
	}

	@Test
	@DisplayName("Lists all saved plans")
	void shouldListAllPlans() {
		repositoryAdapter.save(planWithName("Silver"));
		repositoryAdapter.save(planWithName("Gold"));

		assertThat(repositoryAdapter.findAll()).extracting(PlanDomain::getName).contains("Silver", "Gold");
	}

	@Test
	@DisplayName("Deletes a saved plan")
	void shouldDeletePlan() {
		PlanDomain saved = repositoryAdapter.save(planWithName("Bronze"));

		repositoryAdapter.deleteById(saved.getId());

		assertThat(repositoryAdapter.findById(saved.getId())).isEmpty();
	}

	private PlanDomain planWithName(String name) {
		PlanDomain plan = PlanDomain.builder()
				.name(name)
				.tier(PlanTier.SILVER)
				.priceMonthly(BigDecimal.TEN)
				.priceAnnual(BigDecimal.ONE)
				.features(List.of("x"))
				.build();
		plan.setId(UUID.randomUUID());
		return plan;
	}
}
