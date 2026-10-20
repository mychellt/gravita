package br.gravita.core.usercases.system;

import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

	@Mock
	private SessionStorePort sessionStorePort;
	@InjectMocks
	private LogoutService service;

	@Test
	@DisplayName("Revokes the session behind the token")
	void shouldRevokeTheSession() {
		service.execute("token");

		verify(sessionStorePort).revoke("token");
	}

	@Test
	@DisplayName("A missing or blank token never reaches the session store")
	void shouldIgnoreAMissingToken() {
		service.execute(null);
		service.execute(" ");

		verifyNoInteractions(sessionStorePort);
	}
}
