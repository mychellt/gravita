package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.ActivationRejectedException;
import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@UseCase
public class ActivateAccountService implements ActivateAccountUseCase {

	private final ActivationTokenRepositoryPort tokenRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;
	private final Clock clock;

	@Autowired
	public ActivateAccountService(ActivationTokenRepositoryPort tokenRepositoryPort,
			UserRepositoryPort userRepositoryPort) {
		this(tokenRepositoryPort, userRepositoryPort, Clock.systemUTC());
	}

	public ActivateAccountService(ActivationTokenRepositoryPort tokenRepositoryPort,
			UserRepositoryPort userRepositoryPort, Clock clock) {
		this.tokenRepositoryPort = tokenRepositoryPort;
		this.userRepositoryPort = userRepositoryPort;
		this.clock = clock;
	}

	/** Token and user change in one transaction: a rejection leaves both untouched. */
	@Override
	@Transactional
	public void execute(String rawToken) {
		ActivationToken token = findToken(rawToken);
		token.consume(Instant.now(clock));

		User user = userRepositoryPort.findById(UserId.of(token.getUserId()))
				.orElseThrow(ActivateAccountService::invalidToken);
		user.activate();

		userRepositoryPort.update(user);
		tokenRepositoryPort.save(token);
	}

	private ActivationToken findToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw invalidToken();
		}
		return tokenRepositoryPort.findByTokenHashForUpdate(ActivationToken.hash(rawToken.strip()))
				.orElseThrow(ActivateAccountService::invalidToken);
	}

	private static ActivationRejectedException invalidToken() {
		return new ActivationRejectedException(Reason.INVALID, "Link de ativação inválido.");
	}
}
