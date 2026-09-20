package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.DeletePaymentMethodPort;
import br.gravita.core.ports.outbound.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeletePaymentMethodAdapter implements DeletePaymentMethodPort {

	private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;

	public DeletePaymentMethodAdapter(PaymentMethodRepositoryPort paymentMethodRepositoryPort) {
		this.paymentMethodRepositoryPort = paymentMethodRepositoryPort;
	}

	@Override
	public Void execute(Context context) {
		UUID id = context.getData(UUID.class);
		paymentMethodRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + id));
		paymentMethodRepositoryPort.deleteById(id);
		return null;
	}
}
