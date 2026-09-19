package br.gravita.system.application.port.out;

import br.gravita.system.domain.model.User;
import br.gravita.system.domain.model.UserId;

import java.util.Optional;

public interface UserRepositoryPort {

	User save(User user);

	void update(User user);

	Optional<User> findById(UserId userId);

	boolean existsByEmail(String email);
}
