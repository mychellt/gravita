package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PlanFeatureEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.PlanLimitsEmbeddable;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanFeature;
import br.gravita.core.domain.PlanLimits;
import br.gravita.core.domain.PlanSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.gravita.core.domain.PlanFixtures.aPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PlanPersistenceMapperTest {

	private final PlanPersistenceMapper mapper = new PlanPersistenceMapperImpl();

	@Test
	@DisplayName("Maps every plan field to the entity and back")
	void shouldRoundTripPlan() {
		PlanDomain plan = aPlan()
				.featured(true)
				.limits(new PlanLimits(2, null, 5, null))
				.support(new PlanSupport(true, true, false, 12, false, true))
				.build();

		PlanDomain roundTripped = mapper.map(mapper.map(plan));

		assertThat(roundTripped).usingRecursiveComparison().isEqualTo(plan);
	}

	@Test
	@DisplayName("Maps feature rows with their label, included flag and display order")
	void shouldMapFeatureRows() {
		PlanDomain plan = aPlan()
				.features(List.of(new PlanFeature("API", false, 1), new PlanFeature("1 CNPJ", true, 0)))
				.build();

		PlanJpaEntity entity = mapper.map(plan);

		assertThat(entity.getFeatures())
				.extracting(PlanFeatureEmbeddable::getLabel, PlanFeatureEmbeddable::isIncluded, PlanFeatureEmbeddable::getDisplayOrder)
				.containsExactly(tuple("API", false, 1), tuple("1 CNPJ", true, 0));
	}

	@Test
	@DisplayName("Reads an entity whose limits are all unlimited as four null limits, not as missing limits")
	void shouldMapUnlimitedLimitsWhenHibernateLeavesEmbeddableNull() {
		PlanJpaEntity entity = mapper.map(aPlan().limits(new PlanLimits(null, null, null, null)).build());
		entity.setLimits(null);

		PlanDomain plan = mapper.map(entity);

		assertThat(plan.getLimits()).isEqualTo(new PlanLimits(null, null, null, null));
		assertThat(entity.getLimits()).isInstanceOf(PlanLimitsEmbeddable.class);
	}
}
