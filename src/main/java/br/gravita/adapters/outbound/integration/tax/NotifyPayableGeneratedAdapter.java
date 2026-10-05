package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort;
import org.springframework.stereotype.Component;

@Component
class NotifyPayableGeneratedAdapter implements NotifyPayableGeneratedPort {

	@Override
	public void notifyGenerated(final NotifyPayableGeneratedCommand command) {
	}
}
