package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.ActivationEmailRequested;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.PublishActivationEmailRequestedPort;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Decides whether a resend is due and hands the actual work (new token, old ones expired, mail) to the
 * after-commit {@code ActivationEmailRequested} listener, the same async path the signup uses.
 */
@UseCase
public class ResendActivationService implements ResendActivationUseCase {

	static final Duration COOLDOWN = Duration.ofSeconds(60);

	private final UserRepositoryPort userRepositoryPort;
	private final ActivationTokenRepositoryPort tokenRepositoryPort;
	private final PublishActivationEmailRequestedPort publishPort;
	private final Clock clock;

	@Autowired
	public ResendActivationService(UserRepositoryPort userRepositoryPort,
			ActivationTokenRepositoryPort tokenRepositoryPort, PublishActivationEmailRequestedPort publishPort) {
		this(userRepositoryPort, tokenRepositoryPort, publishPort, Clock.systemUTC());
	}

	public ResendActivationService(UserRepositoryPort userRepositoryPort,
			ActivationTokenRepositoryPort tokenRepositoryPort, PublishActivationEmailRequestedPort publishPort,
			Clock clock) {
		this.userRepositoryPort = userRepositoryPort;
		this.tokenRepositoryPort = tokenRepositoryPort;
		this.publishPort = publishPort;
		this.clock = clock;
	}

	/** Transactional only so the published event is bound to the commit, like the signup's. */
	@Override
	@Transactional
	public void execute(String email) {
		if (email == null || email.isBlank()) {
			return;
		}
		userRepositoryPort.findByEmail(email.strip())
				.filter(user -> user.getStatus() == UserStatus.PENDING_ACTIVATION)
				.filter(this::isOutsideCooldown)
				.ifPresent(user -> publishPort.publish(
						new ActivationEmailRequested(user.getId(), user.getName(), user.getEmail())));
	}

	private boolean isOutsideCooldown(User user) {
		Instant now = Instant.now(clock);
		return tokenRepositoryPort.findLatestByUserId(user.getId().value())
				.map(latest -> !latest.wasIssuedWithin(COOLDOWN, now))
				.orElse(true);
	}
}
