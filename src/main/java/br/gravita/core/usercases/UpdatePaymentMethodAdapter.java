package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdatePaymentMethodPort;
import br.gravita.core.ports.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdatePaymentMethodAdapter implements UpdatePaymentMethodPort {

	private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;

	public UpdatePaymentMethodAdapter(PaymentMethodRepositoryPort paymentMethodRepositoryPort) {
		this.paymentMethodRepositoryPort = paymentMethodRepositoryPort;
	}

	@Override
	public PaymentMethodDomain execute(Context context) {
		PaymentMethodDomain paymentMethod = context.getData(PaymentMethodDomain.class);
		paymentMethodRepositoryPort.get(paymentMethod.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + paymentMethod.getId()));
		return paymentMethodRepositoryPort.save(paymentMethod);
	}
}
