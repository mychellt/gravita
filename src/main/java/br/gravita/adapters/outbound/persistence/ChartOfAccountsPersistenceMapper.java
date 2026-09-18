package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import br.gravita.core.domain.ChartOfAccountsDomain;

class ChartOfAccountsPersistenceMapper {

	ChartOfAccountsDomain toDomain(ChartOfAccountsJpaEntity entity) {
		return ChartOfAccountsDomain.builder()
				.id(entity.getId())
				.code(entity.getCode())
				.name(entity.getName())
				.accountType(entity.getAccountType())
				.parentId(entity.getParentId())
				.build();
	}

	ChartOfAccountsJpaEntity toEntity(ChartOfAccountsDomain domain) {
		return ChartOfAccountsJpaEntity.builder()
				.id(domain.getId())
				.code(domain.getCode())
				.name(domain.getName())
				.accountType(domain.getAccountType())
				.parentId(domain.getParentId())
				.build();
	}
}
