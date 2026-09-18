package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.IbgeMunicipalityJpaEntity;
import br.gravita.core.domain.IbgeMunicipalityDomain;

class IbgeMunicipalityPersistenceMapper {

	IbgeMunicipalityDomain toDomain(IbgeMunicipalityJpaEntity entity) {
		return IbgeMunicipalityDomain.builder()
				.id(entity.getId())
				.ibgeCode(entity.getIbgeCode())
				.name(entity.getName())
				.stateCode(entity.getStateCode())
				.build();
	}

	IbgeMunicipalityJpaEntity toEntity(IbgeMunicipalityDomain domain) {
		return IbgeMunicipalityJpaEntity.builder()
				.id(domain.getId())
				.ibgeCode(domain.getIbgeCode())
				.name(domain.getName())
				.stateCode(domain.getStateCode())
				.build();
	}
}
