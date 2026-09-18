package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.InterstateIcmsRateJpaEntity;
import br.gravita.core.domain.InterstateIcmsRateDomain;

class InterstateIcmsRatePersistenceMapper {

	InterstateIcmsRateDomain toDomain(InterstateIcmsRateJpaEntity entity) {
		return InterstateIcmsRateDomain.builder()
				.id(entity.getId())
				.originState(entity.getOriginState())
				.destinationState(entity.getDestinationState())
				.ratePercent(entity.getRatePercent())
				.build();
	}

	InterstateIcmsRateJpaEntity toEntity(InterstateIcmsRateDomain domain) {
		return InterstateIcmsRateJpaEntity.builder()
				.id(domain.getId())
				.originState(domain.getOriginState())
				.destinationState(domain.getDestinationState())
				.ratePercent(domain.getRatePercent())
				.build();
	}
}
