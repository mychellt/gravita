package br.gravita.adapters.outbound.security;

import br.gravita.core.domain.system.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionStoreAdapterTest {

	private final InMemorySessionStoreAdapter adapter = new InMemorySessionStoreAdapter();

	@Test
	@DisplayName("Resolves a stored token back to its user")
	void shouldResolveAStoredTokenBackToItsUser() {
		UserId userId = UserId.generate();

		adapter.store("token-1", userId);

		assertThat(adapter.resolve("token-1")).contains(userId);
	}

	@Test
	@DisplayName("Returns empty for an unknown token")
	void shouldReturnEmptyForAnUnknownToken() {
		assertThat(adapter.resolve("does-not-exist")).isEmpty();
	}
}
