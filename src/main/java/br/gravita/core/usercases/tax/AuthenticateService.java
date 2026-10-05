package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.ports.outbound.security.TotpVerificationPort;
import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthenticateCommand;
import br.gravita.core.usercases.system.AuthenticateUseCase;

import java.util.Optional;
import java.util.UUID;

@UseCase
public class AuthenticateService implements AuthenticateUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final TotpVerificationPort totpVerificationPort;
	private final AccessLogRepositoryPort accessLogRepositoryPort;
	private final PasswordVerificationPort passwordVerificationPort;
	private final SessionStorePort sessionStorePort;

	public AuthenticateService(final UserRepositoryPort userRepositoryPort, final TotpVerificationPort totpVerificationPort,
			final AccessLogRepositoryPort accessLogRepositoryPort, final PasswordVerificationPort passwordVerificationPort,
			final SessionStorePort sessionStorePort) {
		this.userRepositoryPort = userRepositoryPort;
		this.totpVerificationPort = totpVerificationPort;
		this.accessLogRepositoryPort = accessLogRepositoryPort;
		this.passwordVerificationPort = passwordVerificationPort;
		this.sessionStorePort = sessionStorePort;
	}

	@Override
	public AuthResult execute(final AuthenticateCommand command) {
		final Optional<User> maybeUser = userRepositoryPort.findByEmail(command.email());
		if (maybeUser.isEmpty()) {
			return reject(null, command);
		}

		final User user = maybeUser.get();
		final boolean credentialsValid = user.getStatus() == UserStatus.ACTIVE
				&& passwordVerificationPort.matches(command.rawPassword(), user.getRawPassword());
		if (!credentialsValid) {
			return reject(user.getId(), command);
		}

		if (user.isTwoFactorEnabled()) {
			if (command.totpCode() == null || command.totpCode().isBlank()) {
				return AuthResult.totpRequired();
			}
			if (!totpVerificationPort.verify(user.getId(), command.totpCode())) {
				return reject(user.getId(), command);
			}
		}

		return authenticate(user.getId(), command);
	}

	private AuthResult reject(final UserId userId, final AuthenticateCommand command) {
		accessLogRepositoryPort.save(AccessLog.login(userId, command.email(), false, command.ip(), command.device()));
		return AuthResult.rejected();
	}

	private AuthResult authenticate(final UserId userId, final AuthenticateCommand command) {
		accessLogRepositoryPort.save(AccessLog.login(userId, command.email(), true, command.ip(), command.device()));
		final String sessionToken = UUID.randomUUID().toString();
		sessionStorePort.store(sessionToken, userId);
		return AuthResult.authenticated(sessionToken);
	}
}
