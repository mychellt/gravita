package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;

class UserPersistenceMapper {

	UserJpaEntity toEntity(User domain, String passwordHash) {
		return UserJpaEntity.builder()
				.id(domain.getId().value())
				.name(domain.getName())
				.email(domain.getEmail())
				.passwordHash(passwordHash)
				.profileId(domain.getProfileId())
				.twoFactorEnabled(domain.isTwoFactorEnabled())
				.status(domain.getStatus())
				.build();
	}

	User toDomain(UserJpaEntity entity) {
		return User.builder()
				.id(UserId.of(entity.getId()))
				.name(entity.getName())
				.email(entity.getEmail())
				.rawPassword(entity.getPasswordHash())
				.profileId(entity.getProfileId())
				.twoFactorEnabled(entity.isTwoFactorEnabled())
				.status(entity.getStatus())
				.build();
	}
}
