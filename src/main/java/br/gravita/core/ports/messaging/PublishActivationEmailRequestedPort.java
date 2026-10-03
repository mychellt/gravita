package br.gravita.core.ports.messaging;

import br.gravita.core.domain.system.ActivationEmailRequested;

/** Same contract as {@link PublishUserSignedUpPort}: subscribers must only act once the publisher's transaction commits. */
public interface PublishActivationEmailRequestedPort {

	void publish(ActivationEmailRequested event);
}
