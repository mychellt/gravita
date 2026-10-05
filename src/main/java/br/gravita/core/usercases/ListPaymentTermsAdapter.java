package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.ports.business.ListPaymentTermsPort;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListPaymentTermsAdapter implements ListPaymentTermsPort {

	private final PaymentTermRepositoryPort paymentTermRepositoryPort;

	public ListPaymentTermsAdapter(final PaymentTermRepositoryPort paymentTermRepositoryPort) {
		this.paymentTermRepositoryPort = paymentTermRepositoryPort;
	}

	@Override
	public List<PaymentTermDomain> execute(final Context context) {
		return paymentTermRepositoryPort.findAll();
	}
}
