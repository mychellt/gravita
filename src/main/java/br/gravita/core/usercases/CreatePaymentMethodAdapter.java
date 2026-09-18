package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.business.CreatePaymentMethodPort;
import br.gravita.core.ports.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreatePaymentMethodAdapter implements CreatePaymentMethodPort {

	private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;

	public CreatePaymentMethodAdapter(PaymentMethodRepositoryPort paymentMethodRepositoryPort) {
		this.paymentMethodRepositoryPort = paymentMethodRepositoryPort;
	}

	@Override
	public PaymentMethodDomain execute(Context context) {
		PaymentMethodDomain paymentMethod = context.getData(PaymentMethodDomain.class);
		if (paymentMethod.getId() == null) {
			paymentMethod.setId(UUID.randomUUID());
		}
		return paymentMethodRepositoryPort.save(paymentMethod);
	}
}
