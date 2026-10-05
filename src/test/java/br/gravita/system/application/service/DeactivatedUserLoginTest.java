package br.gravita.system.application.service;

import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.ports.outbound.security.TotpVerificationPort;
import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;
import br.gravita.core.usercases.system.AuthenticateCommand;
import br.gravita.core.usercases.system.UpdateUserCommand;
import br.gravita.core.usercases.tax.AuthenticateService;
import br.gravita.core.usercases.tax.UpdateUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

/** Inactivating a user through {@code PATCH /api/users/{id}} must lock the account out of login. */
@ExtendWith(MockitoExtension.class)
class DeactivatedUserLoginTest {

	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");

	@Mock
	private UserRepositoryPort userRepositoryPort;
	@Mock
	private ProfileRepositoryPort profileRepositoryPort;
	@Mock
	private TotpVerificationPort totpVerificationPort;
	@Mock
	private AccessLogRepositoryPort accessLogRepositoryPort;
	@Mock
	private PasswordVerificationPort passwordVerificationPort;
	@Mock
	private SessionStorePort sessionStorePort;

	@Test
	@DisplayName("A user flipped to INACTIVE can no longer authenticate, even with the correct password")
	void shouldLockOutAUserInactivatedThroughTheUpdateUseCase() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON, UUID.randomUUID());
		final Map<UserId, User> store = new HashMap<>(Map.of(user.getId(), user));
		lenient().when(userRepositoryPort.findById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.<UserId>getArgument(0))));
		lenient().when(userRepositoryPort.findByEmail("jane@example.com")).thenAnswer(inv -> Optional.of(store.get(user.getId())));
		doAnswer(inv -> store.put(user.getId(), inv.getArgument(0))).when(userRepositoryPort).update(any());
		lenient().when(passwordVerificationPort.matches("s3cret!", user.getRawPassword())).thenReturn(true);
		final AuthenticateService authenticate = new AuthenticateService(userRepositoryPort, totpVerificationPort,
				accessLogRepositoryPort, passwordVerificationPort, sessionStorePort);
		final AuthenticateCommand login = new AuthenticateCommand("jane@example.com", "s3cret!", null, "1.2.3.4", "Chrome");
		assertThat(authenticate.execute(login).status()).isEqualTo(AuthStatus.AUTHENTICATED);

		new UpdateUserService(userRepositoryPort, profileRepositoryPort)
				.execute(new UpdateUserCommand(user.getId().value(), null, null, null, UserStatus.INACTIVE));

		assertThat(store.get(user.getId()).getStatus()).isEqualTo(UserStatus.INACTIVE);
		final AuthResult afterwards = authenticate.execute(login);
		assertThat(afterwards.status()).isEqualTo(AuthStatus.REJECTED);
		assertThat(afterwards.sessionToken()).isNull();
	}
}
