package br.gravita.system.application.port.out;

import br.gravita.system.domain.model.User;

public interface UserRepositoryPort {

	User save(User user);

	boolean existsByEmail(String email);
}
