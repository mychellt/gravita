package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.IssuedActivationToken;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.messaging.ActivationEmailRequest;
import br.gravita.core.ports.messaging.SendActivationEmailPort;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;

/**
 * Issues a fresh activation token (expiring any still-open earlier one) and e-mails it. Deliberately not transactional: the token must be committed
 * before the mail leaves, otherwise a rolled-back token could be mailed as a link that never works.
 */
@UseCase
public class SendActivationEmailService implements SendActivationEmailUseCase {

	private final ActivationTokenRepositoryPort tokenRepositoryPort;
	private final SendActivationEmailPort sendActivationEmailPort;
	private final Clock clock;

	@Autowired
	public SendActivationEmailService(ActivationTokenRepositoryPort tokenRepositoryPort,
			SendActivationEmailPort sendActivationEmailPort) {
		this(tokenRepositoryPort, sendActivationEmailPort, Clock.systemUTC());
	}

	public SendActivationEmailService(ActivationTokenRepositoryPort tokenRepositoryPort,
			SendActivationEmailPort sendActivationEmailPort, Clock clock) {
		this.tokenRepositoryPort = tokenRepositoryPort;
		this.sendActivationEmailPort = sendActivationEmailPort;
		this.clock = clock;
	}

	@Override
	public void execute(SendActivationEmailCommand command) {
		IssuedActivationToken issued = ActivationToken.issue(command.userId(), Instant.now(clock));
		ActivationToken token = tokenRepositoryPort.save(issued.token());
		expirePreviousTokens(command.userId(), token);

		sendActivationEmailPort.send(new ActivationEmailRequest(command.email(), command.name(),
				issued.rawToken(), token.getExpiresAt()));
	}

	/** Only the newest link may work: older open ones (a resend) are expired once the new one is safely stored. */
	private void expirePreviousTokens(UserId userId, ActivationToken current) {
		Instant now = Instant.now(clock);
		for (ActivationToken previous : tokenRepositoryPort.findUnusedByUserId(userId.value())) {
			if (!previous.getId().equals(current.getId())) {
				previous.expire(now);
				tokenRepositoryPort.save(previous);
			}
		}
	}
}
