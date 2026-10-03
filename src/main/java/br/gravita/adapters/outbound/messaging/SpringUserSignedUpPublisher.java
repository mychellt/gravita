package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.system.UserSignedUp;
import br.gravita.core.ports.messaging.PublishUserSignedUpPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes through Spring's event bus so listeners can bind to the signup transaction's commit. */
@Component
public class SpringUserSignedUpPublisher implements PublishUserSignedUpPort {

	private final ApplicationEventPublisher eventPublisher;

	public SpringUserSignedUpPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void publish(UserSignedUp event) {
		eventPublisher.publishEvent(event);
	}
}
