package br.gravita.system.application.service;

import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.ports.outbound.security.TotpVerificationPort;
import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;
import br.gravita.core.usercases.system.AuthenticateCommand;
import br.gravita.core.usercases.tax.AuthenticateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTest {

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private TotpVerificationPort totpVerificationPort;

	@Mock
	private AccessLogRepositoryPort accessLogRepositoryPort;

	@Mock
	private PasswordVerificationPort passwordVerificationPort;

	@Mock
	private SessionStorePort sessionStorePort;

	private AuthenticateService service() {
		return new AuthenticateService(userRepositoryPort, totpVerificationPort, accessLogRepositoryPort,
				passwordVerificationPort, sessionStorePort);
	}

	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");

	private User activeUser(boolean twoFactorEnabled) {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		if (twoFactorEnabled) {
			user.update(null, null, new ProfileReference(SALESPERSON.id(), "Administrator"), null);
		}
		return user;
	}

	@Test
	@DisplayName("Rejects an unknown email without revealing that it is unknown")
	void shouldRejectWhenEmailIsUnknownWithoutRevealingIt() {
		when(userRepositoryPort.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		AuthResult result = service().execute(new AuthenticateCommand("ghost@example.com", "whatever", null, "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.REJECTED);
		assertThat(result.sessionToken()).isNull();
		ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
		verify(accessLogRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isNull();
		assertThat(captor.getValue().getEmail()).isEqualTo("ghost@example.com");
		assertThat(captor.getValue().isSuccessful()).isFalse();
		assertThat(captor.getValue().getIp()).isEqualTo("1.2.3.4");
		assertThat(captor.getValue().getDevice()).isEqualTo("Chrome");
	}

	@Test
	@DisplayName("Rejects a wrong password with the same outcome as an unknown email")
	void shouldRejectWrongPasswordWithTheSameOutcomeAsUnknownEmail() {
		User user = activeUser(false);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordVerificationPort.matches("wrong", user.getRawPassword())).thenReturn(false);

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "wrong", null, "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.REJECTED);
		ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
		verify(accessLogRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
		assertThat(captor.getValue().isSuccessful()).isFalse();
	}

	@Test
	@DisplayName("Rejects an inactive user even with the correct password")
	void shouldRejectInactiveUserEvenWithCorrectPassword() {
		User user = activeUser(false);
		user.update(null, null, null, UserStatus.INACTIVE);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", null, "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.REJECTED);
		verify(passwordVerificationPort, never()).matches(any(), any());
	}

	@Test
	@DisplayName("Authenticates directly when two-factor is not enabled")
	void shouldAuthenticateDirectlyWhenTwoFactorIsNotEnabled() {
		User user = activeUser(false);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordVerificationPort.matches("s3cret!", user.getRawPassword())).thenReturn(true);

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", null, "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.AUTHENTICATED);
		assertThat(result.sessionToken()).isNotBlank();
		ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
		verify(accessLogRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().isSuccessful()).isTrue();
		assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
		verify(sessionStorePort).store(result.sessionToken(), user.getId());
	}

	@Test
	@DisplayName("Requires a TOTP code when two-factor is enabled and none is provided")
	void shouldRequireTotpWhenTwoFactorIsEnabledAndNoCodeProvided() {
		User user = activeUser(true);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordVerificationPort.matches("s3cret!", user.getRawPassword())).thenReturn(true);

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", null, "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.TOTP_REQUIRED);
		assertThat(result.sessionToken()).isNull();
		verify(accessLogRepositoryPort, never()).save(any());
		verify(totpVerificationPort, never()).verify(any(), any());
	}

	@Test
	@DisplayName("Refuses to issue a session when the TOTP code is invalid")
	void shouldRejectSessionIssuanceWhenTotpCodeIsInvalid() {
		User user = activeUser(true);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordVerificationPort.matches("s3cret!", user.getRawPassword())).thenReturn(true);
		when(totpVerificationPort.verify(eq(user.getId()), eq("000000"))).thenReturn(false);

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", "000000", "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.REJECTED);
		ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
		verify(accessLogRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().isSuccessful()).isFalse();
	}

	@Test
	@DisplayName("Authenticates after a valid TOTP code")
	void shouldAuthenticateAfterAValidTotpCode() {
		User user = activeUser(true);
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordVerificationPort.matches("s3cret!", user.getRawPassword())).thenReturn(true);
		when(totpVerificationPort.verify(eq(user.getId()), eq("123456"))).thenReturn(true);

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", "123456", "1.2.3.4", "Chrome"));

		assertThat(result.status()).isEqualTo(AuthStatus.AUTHENTICATED);
		assertThat(result.sessionToken()).isNotBlank();
		ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
		verify(accessLogRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().isSuccessful()).isTrue();
		verify(sessionStorePort).store(result.sessionToken(), user.getId());
	}

	@Test
	@DisplayName("Rejects a user pending activation even with the correct password (no change to the service needed)")
	void shouldRejectUserPendingActivationEvenWithCorrectPassword() {
		User user = User.signUp("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON, UUID.randomUUID());
		when(userRepositoryPort.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		AuthResult result = service().execute(new AuthenticateCommand(user.getEmail(), "s3cret!", null, "1.2.3.4", "Chrome"));

		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
		assertThat(result.status()).isEqualTo(AuthStatus.REJECTED);
		assertThat(result.sessionToken()).isNull();
		verify(passwordVerificationPort, never()).matches(any(), any());
	}
}
