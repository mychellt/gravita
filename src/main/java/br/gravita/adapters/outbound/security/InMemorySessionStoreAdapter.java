package br.gravita.adapters.outbound.security;

import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP session store: an in-process map from session token to {@link UserId}, populated by
 * AuthenticateUseCase on login. Sessions are lost on restart, never expire, and aren't shared
 * across instances - swap for a persisted/expiring store before running more than one app
 * instance or requiring logout/expiry semantics.
 */
@Component
public class InMemorySessionStoreAdapter implements SessionStorePort {

	private final Map<String, UserId> sessions = new ConcurrentHashMap<>();

	@Override
	public void store(String sessionToken, UserId userId) {
		sessions.put(sessionToken, userId);
	}

	@Override
	public Optional<UserId> resolve(String sessionToken) {
		return Optional.ofNullable(sessions.get(sessionToken));
	}
}
