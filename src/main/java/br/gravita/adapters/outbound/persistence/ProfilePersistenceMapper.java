package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PermissionJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;

import java.util.ArrayList;

class ProfilePersistenceMapper {

	ProfileDomain toDomain(ProfileJpaEntity entity) {
		return ProfileDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.permissions(entity.getPermissions().stream().map(this::toDomain).toList())
				.build();
	}

	ProfileJpaEntity toEntity(ProfileDomain domain) {
		return ProfileJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.permissions(new ArrayList<>(domain.getPermissions().stream().map(this::toEntity).toList()))
				.build();
	}

	private PermissionDomain toDomain(PermissionJpaEntity entity) {
		return new PermissionDomain(entity.getModule(), entity.getScreen(), entity.getAction());
	}

	private PermissionJpaEntity toEntity(PermissionDomain domain) {
		return PermissionJpaEntity.builder()
				.module(domain.getModule())
				.screen(domain.getScreen())
				.action(domain.getAction())
				.build();
	}
}
