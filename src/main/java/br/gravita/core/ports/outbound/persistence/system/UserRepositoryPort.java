package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;

import java.util.Optional;

public interface UserRepositoryPort {

	User save(User user);

	void update(User user);

	Optional<User> findById(UserId userId);

	boolean existsByEmail(String email);
}
