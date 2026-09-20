package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindPaymentTermPort;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindPaymentTermAdapter implements FindPaymentTermPort {

	private final PaymentTermRepositoryPort paymentTermRepositoryPort;

	public FindPaymentTermAdapter(PaymentTermRepositoryPort paymentTermRepositoryPort) {
		this.paymentTermRepositoryPort = paymentTermRepositoryPort;
	}

	@Override
	public PaymentTermDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		return paymentTermRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Payment term not found: " + id));
	}
}
