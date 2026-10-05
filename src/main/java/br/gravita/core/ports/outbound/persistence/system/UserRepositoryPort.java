package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

	User save(User user);

	void update(User user);

	/** Stores the new password of {@code user} (its raw password, hashed on the way in); no other field is touched. */
	void updatePassword(User user);

	Optional<User> findById(UserId userId);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findAllByCompanyId(UUID companyId);
}
