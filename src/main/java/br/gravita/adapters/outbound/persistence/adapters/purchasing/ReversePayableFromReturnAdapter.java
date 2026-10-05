package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort;
import org.springframework.stereotype.Component;

@Component
class ReversePayableFromReturnAdapter implements ReversePayableFromReturnPort {

	@Override
	public void reversePayable(final ReversePayableFromReturnCommand command) {
	}
}
