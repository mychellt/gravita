package br.gravita.adapters.inbound.events;

import br.gravita.adapters.configuration.async.AsyncConfiguration;
import br.gravita.core.domain.system.ActivationEmailRequested;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserSignedUp;
import br.gravita.core.usercases.system.SendActivationEmailCommand;
import br.gravita.core.usercases.system.SendActivationEmailUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the activation e-mail for a new signup (or a resend request). Runs only after the signup commits (a rolled-back signup is never
 * announced to listeners) and on its own thread, so mail latency never delays the signup response and a failure
 * here is logged, never propagated to the client.
 */
@Component
public class ActivationEmailListener {

	private static final Logger log = LoggerFactory.getLogger(ActivationEmailListener.class);

	private final SendActivationEmailUseCase sendActivationEmailUseCase;

	public ActivationEmailListener(SendActivationEmailUseCase sendActivationEmailUseCase) {
		this.sendActivationEmailUseCase = sendActivationEmailUseCase;
	}

	@Async(AsyncConfiguration.ACTIVATION_EMAIL_EXECUTOR)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserSignedUp(UserSignedUp event) {
		send(event.userId(), event.name(), event.email());
	}

	/** A resend takes exactly the signup's path: after commit, own thread, failures logged. */
	@Async(AsyncConfiguration.ACTIVATION_EMAIL_EXECUTOR)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onActivationEmailRequested(ActivationEmailRequested event) {
		send(event.userId(), event.name(), event.email());
	}

	private void send(UserId userId, String name, String email) {
		try {
			sendActivationEmailUseCase.execute(new SendActivationEmailCommand(userId, name, email));
		} catch (RuntimeException e) {
			log.error("Could not send the activation e-mail for user {}", userId.value(), e);
		}
	}
}
