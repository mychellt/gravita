package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.business.ListPaymentMethodsPort;
import br.gravita.core.ports.outbound.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListPaymentMethodsAdapter implements ListPaymentMethodsPort {

	private final PaymentMethodRepositoryPort paymentMethodRepositoryPort;

	public ListPaymentMethodsAdapter(PaymentMethodRepositoryPort paymentMethodRepositoryPort) {
		this.paymentMethodRepositoryPort = paymentMethodRepositoryPort;
	}

	@Override
	public List<PaymentMethodDomain> execute(Context context) {
		return paymentMethodRepositoryPort.findAll();
	}
}
