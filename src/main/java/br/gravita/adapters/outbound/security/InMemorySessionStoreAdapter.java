package br.gravita.adapters.outbound.security;

import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
