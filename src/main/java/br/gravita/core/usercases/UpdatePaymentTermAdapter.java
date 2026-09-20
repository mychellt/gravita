package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdatePaymentTermPort;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdatePaymentTermAdapter implements UpdatePaymentTermPort {

	private final PaymentTermRepositoryPort paymentTermRepositoryPort;

	public UpdatePaymentTermAdapter(PaymentTermRepositoryPort paymentTermRepositoryPort) {
		this.paymentTermRepositoryPort = paymentTermRepositoryPort;
	}

	@Override
	public PaymentTermDomain execute(Context context) {
		PaymentTermDomain paymentTerm = context.getData(PaymentTermDomain.class);
		CreatePaymentTermAdapter.validateInstallments(paymentTerm);
		paymentTermRepositoryPort.get(paymentTerm.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Payment term not found: " + paymentTerm.getId()));
		return paymentTermRepositoryPort.save(paymentTerm);
	}
}
