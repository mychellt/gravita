package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.InterstateIcmsRateJpaEntity;
import br.gravita.core.domain.InterstateIcmsRateDomain;

public class InterstateIcmsRatePersistenceMapper {

	public InterstateIcmsRateDomain toDomain(InterstateIcmsRateJpaEntity entity) {
		return InterstateIcmsRateDomain.builder()
				.id(entity.getId())
				.originState(entity.getOriginState())
				.destinationState(entity.getDestinationState())
				.ratePercent(entity.getRatePercent())
				.build();
	}

	public InterstateIcmsRateJpaEntity toEntity(InterstateIcmsRateDomain domain) {
		return InterstateIcmsRateJpaEntity.builder()
				.id(domain.getId())
				.originState(domain.getOriginState())
				.destinationState(domain.getDestinationState())
				.ratePercent(domain.getRatePercent())
				.build();
	}
}
