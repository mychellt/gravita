package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.system.ActivationEmailRequested;
import br.gravita.core.ports.messaging.PublishActivationEmailRequestedPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringActivationEmailRequestedPublisher implements PublishActivationEmailRequestedPort {

	private final ApplicationEventPublisher eventPublisher;

	public SpringActivationEmailRequestedPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void publish(ActivationEmailRequested event) {
		eventPublisher.publishEvent(event);
	}
}
