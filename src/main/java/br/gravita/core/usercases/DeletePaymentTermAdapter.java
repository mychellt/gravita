package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.DeletePaymentTermPort;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeletePaymentTermAdapter implements DeletePaymentTermPort {

	private final PaymentTermRepositoryPort paymentTermRepositoryPort;

	public DeletePaymentTermAdapter(PaymentTermRepositoryPort paymentTermRepositoryPort) {
		this.paymentTermRepositoryPort = paymentTermRepositoryPort;
	}

	@Override
	public Void execute(Context context) {
		UUID id = context.getData(UUID.class);
		paymentTermRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Payment term not found: " + id));
		paymentTermRepositoryPort.deleteById(id);
		return null;
	}
}
