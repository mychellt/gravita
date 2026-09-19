package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.User;
import br.gravita.system.domain.model.UserId;

class UserPersistenceMapper {

	UserJpaEntity toEntity(User domain, String passwordHash) {
		return UserJpaEntity.builder()
				.id(domain.getId().value())
				.name(domain.getName())
				.email(domain.getEmail())
				.passwordHash(passwordHash)
				.profileId(domain.getProfileId())
				.twoFactorEnabled(domain.isTwoFactorEnabled())
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
				.build();
	}
}
