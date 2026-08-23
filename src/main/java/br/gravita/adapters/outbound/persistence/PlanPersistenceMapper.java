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
		return PlanJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.tier(domain.getTier())
				.priceMonthly(domain.getPriceMonthly())
				.priceAnnual(domain.getPriceAnnual())
				.features(domain.getFeatures())
				.build();
	}
}
