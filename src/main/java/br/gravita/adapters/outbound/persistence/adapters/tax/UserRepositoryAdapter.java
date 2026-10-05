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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class UserRepositoryAdapter implements UserRepositoryPort {

	private final UserJpaRepository jpaRepository;
	private final PasswordHasher passwordHasher;
	private final UserPersistenceMapper mapper;

	UserRepositoryAdapter(final UserJpaRepository jpaRepository, final PasswordHasher passwordHasher, final UserPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.passwordHasher = passwordHasher;
		this.mapper = mapper;
	}

	@Override
	public User save(final User user) {
		final String passwordHash = passwordHasher.hash(user.getRawPassword());
		final UserJpaEntity saved = jpaRepository.save(mapper.map(user, passwordHash));
		return mapper.map(saved);
	}

	@Override
	public void update(final User user) {
		final UserJpaEntity entity = jpaRepository.findById(user.getId().value())
				.orElseThrow(() -> new UserNotFoundException(user.getId().value()));
		entity.setName(user.getName());
		entity.setEmail(user.getEmail());
		entity.setProfileId(user.getProfileId());
		entity.setTwoFactorEnabled(user.isTwoFactorEnabled());
		entity.setStatus(user.getStatus());
		jpaRepository.save(entity);
	}

	@Override
	public void updatePassword(final User user) {
		final UserJpaEntity entity = jpaRepository.findById(user.getId().value())
				.orElseThrow(() -> new UserNotFoundException(user.getId().value()));
		entity.setPasswordHash(passwordHasher.hash(user.getRawPassword()));
		jpaRepository.save(entity);
	}

	@Override
	public Optional<User> findById(final UserId userId) {
		return jpaRepository.findById(userId.value()).map(mapper::map);
	}

	@Override
	public Optional<User> findByEmail(final String email) {
		return jpaRepository.findByEmail(email).map(mapper::map);
	}

	@Override
	public boolean existsByEmail(final String email) {
		return jpaRepository.existsByEmail(email);
	}

	@Override
	public List<User> findAllByCompanyId(final UUID companyId) {
		return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::map).toList();
	}
}
