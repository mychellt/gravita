package br.gravita.core.ports.outbound.security;

import br.gravita.core.domain.system.UserId;

import java.util.Optional;

public interface SessionStorePort {

	void store(String sessionToken, UserId userId);

	Optional<UserId> resolve(String sessionToken);
}
