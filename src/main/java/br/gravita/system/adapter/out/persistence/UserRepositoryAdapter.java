package br.gravita.system.adapter.out.persistence;

import br.gravita.shared.PersistenceAdapter;
import br.gravita.system.application.port.out.UserRepositoryPort;
import br.gravita.system.domain.model.User;

@PersistenceAdapter
class UserRepositoryAdapter implements UserRepositoryPort {

	private final UserJpaRepository jpaRepository;
	private final PasswordHasher passwordHasher;
	private final UserPersistenceMapper mapper = new UserPersistenceMapper();

	UserRepositoryAdapter(UserJpaRepository jpaRepository, PasswordHasher passwordHasher) {
		this.jpaRepository = jpaRepository;
		this.passwordHasher = passwordHasher;
	}

	@Override
	public User save(User user) {
		String passwordHash = passwordHasher.hash(user.getRawPassword());
		UserJpaEntity saved = jpaRepository.save(mapper.toEntity(user, passwordHash));
		return mapper.toDomain(saved);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpaRepository.existsByEmail(email);
	}
}
