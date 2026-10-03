package br.gravita.core.ports.messaging;

import br.gravita.core.domain.system.UserSignedUp;

/**
 * Announces a completed signup. The announcement is tied to the publisher's transaction: subscribers must only act
 * once it commits, so a rolled-back signup never triggers any side effect.
 */
public interface PublishUserSignedUpPort {

	void publish(UserSignedUp event);
}
