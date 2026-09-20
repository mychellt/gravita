package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.CostCenterJpaEntity;
import br.gravita.core.domain.CostCenterDomain;

class CostCenterPersistenceMapper {

	CostCenterDomain toDomain(CostCenterJpaEntity entity) {
		return CostCenterDomain.builder()
				.id(entity.getId())
				.code(entity.getCode())
				.name(entity.getName())
				.parentId(entity.getParentId())
				.build();
	}

	CostCenterJpaEntity toEntity(CostCenterDomain domain) {
		return CostCenterJpaEntity.builder()
				.id(domain.getId())
				.code(domain.getCode())
				.name(domain.getName())
				.parentId(domain.getParentId())
				.build();
	}
}
