package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindPaymentMethodPort;
import br.gravita.core.ports.outbound.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindPaymentMethodAdapter implements FindPaymentMethodPort {

	private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;

	public FindPaymentMethodAdapter(final PaymentMethodRepositoryPort paymentMethodRepositoryPort) {
		this.paymentMethodRepositoryPort = paymentMethodRepositoryPort;
	}

	@Override
	public PaymentMethodDomain execute(final Context context) {
		final UUID id = context.getData(UUID.class);
		return paymentMethodRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + id));
	}
}
