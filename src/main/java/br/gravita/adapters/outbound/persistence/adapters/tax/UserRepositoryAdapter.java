package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.UserPersistenceMapper;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.adapters.outbound.security.PasswordHasher;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;

import java.util.Optional;

@PersistenceAdapter
class UserRepositoryAdapter implements UserRepositoryPort {

	private final UserJpaRepository jpaRepository;
	private final PasswordHasher passwordHasher;
	private final UserPersistenceMapper mapper;

	UserRepositoryAdapter(UserJpaRepository jpaRepository, PasswordHasher passwordHasher, UserPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.passwordHasher = passwordHasher;
		this.mapper = mapper;
	}

	@Override
	public User save(User user) {
		String passwordHash = passwordHasher.hash(user.getRawPassword());
		UserJpaEntity saved = jpaRepository.save(mapper.toEntity(user, passwordHash));
		return mapper.toDomain(saved);
	}

	@Override
	public void update(User user) {
		UserJpaEntity entity = jpaRepository.findById(user.getId().value())
				.orElseThrow(() -> new UserNotFoundException(user.getId().value()));
		entity.setName(user.getName());
		entity.setEmail(user.getEmail());
		entity.setProfileId(user.getProfileId());
		entity.setTwoFactorEnabled(user.isTwoFactorEnabled());
		entity.setStatus(user.getStatus());
		jpaRepository.save(entity);
	}

	@Override
	public Optional<User> findById(UserId userId) {
		return jpaRepository.findById(userId.value()).map(mapper::toDomain);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return jpaRepository.findByEmail(email).map(mapper::toDomain);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpaRepository.existsByEmail(email);
	}
}
