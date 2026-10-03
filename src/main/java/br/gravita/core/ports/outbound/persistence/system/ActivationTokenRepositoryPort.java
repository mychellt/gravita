package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.ActivationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepositoryPort {

	ActivationToken save(ActivationToken token);

	/** Locks the row so two concurrent clicks on the same link cannot both consume it. */
	Optional<ActivationToken> findByTokenHashForUpdate(String tokenHash);

	/** The user's most recently issued token, spent or not. */
	Optional<ActivationToken> findLatestByUserId(UUID userId);

	/** Tokens of the user that have not been consumed yet (they may already be past their expiry). */
	List<ActivationToken> findUnusedByUserId(UUID userId);
}
