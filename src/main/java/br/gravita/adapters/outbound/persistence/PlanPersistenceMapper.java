package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PlanJpaEntity;
import br.gravita.core.domain.PlanDomain;

class PlanPersistenceMapper {

	PlanDomain toDomain(PlanJpaEntity entity) {
		return PlanDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.tier(entity.getTier())
				.priceMonthly(entity.getPriceMonthly())
				.priceAnnual(entity.getPriceAnnual())
				.features(entity.getFeatures())
				.build();
	}

	PlanJpaEntity toEntity(PlanDomain domain) {
		return new PlanJpaEntity(
				domain.getId(),
				domain.getName(),
				domain.getTier(),
				domain.getPriceMonthly(),
				domain.getPriceAnnual(),
				domain.getFeatures());
	}
}
