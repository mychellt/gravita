package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

	private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

	@Mock
	private SessionStorePort sessionStorePort;
	@Mock
	private UserRepositoryPort userRepositoryPort;
	@Mock
	private ProfileRepositoryPort profileRepositoryPort;
	@InjectMocks
	private GetCurrentUserService service;

	@Test
	@DisplayName("Resolves the session to the user's name, e-mail, profile name and company")
	void shouldResolveTheLoggedUser() {
		UUID companyId = UUID.randomUUID();
		User user = User.register("Ana Souza", "ana@acme.com", "s3cret-pass", ADMINISTRATOR, companyId);
		when(sessionStorePort.resolve("token")).thenReturn(Optional.of(user.getId()));
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(ADMINISTRATOR.id())).thenReturn(Optional.of(ADMINISTRATOR));

		assertThat(service.execute("token"))
				.contains(new CurrentUser("Ana Souza", "ana@acme.com", "Administrator", companyId));
	}

	@Test
	@DisplayName("An unknown session has no user")
	void shouldBeEmptyForAnUnknownSession() {
		when(sessionStorePort.resolve("stale")).thenReturn(Optional.empty());

		assertThat(service.execute("stale")).isEmpty();
	}

	@Test
	@DisplayName("A missing or blank token never reaches the session store")
	void shouldBeEmptyWithoutAToken() {
		assertThat(service.execute(null)).isEmpty();
		assertThat(service.execute("  ")).isEmpty();

		verifyNoInteractions(sessionStorePort, userRepositoryPort, profileRepositoryPort);
	}

	@Test
	@DisplayName("A session whose user no longer exists has no user")
	void shouldBeEmptyWhenTheUserIsGone() {
		User user = User.register("Ana Souza", "ana@acme.com", "s3cret-pass", ADMINISTRATOR);
		when(sessionStorePort.resolve("token")).thenReturn(Optional.of(user.getId()));
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.empty());

		assertThat(service.execute("token")).isEmpty();
	}
}
