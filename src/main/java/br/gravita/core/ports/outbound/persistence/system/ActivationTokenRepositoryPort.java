package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.ActivationToken;

import java.util.Optional;

public interface ActivationTokenRepositoryPort {

	ActivationToken save(ActivationToken token);

	/** Locks the row so two concurrent clicks on the same link cannot both consume it. */
	Optional<ActivationToken> findByTokenHashForUpdate(String tokenHash);
}
