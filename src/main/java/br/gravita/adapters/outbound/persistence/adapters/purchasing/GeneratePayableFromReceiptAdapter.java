package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import org.springframework.stereotype.Component;

@Component
class GeneratePayableFromReceiptAdapter implements GeneratePayableFromReceiptPort {

	@Override
	public void generatePayables(GeneratePayableFromReceiptCommand command) {
	}
}
