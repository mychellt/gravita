package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.CreatePaymentTermPort;
import br.gravita.core.ports.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreatePaymentTermAdapter implements CreatePaymentTermPort {

	private final PaymentTermRepositoryPort paymentTermRepositoryPort;

	public CreatePaymentTermAdapter(PaymentTermRepositoryPort paymentTermRepositoryPort) {
		this.paymentTermRepositoryPort = paymentTermRepositoryPort;
	}

	@Override
	public PaymentTermDomain execute(Context context) {
		PaymentTermDomain paymentTerm = context.getData(PaymentTermDomain.class);
		validateInstallments(paymentTerm);
		if (paymentTerm.getId() == null) {
			paymentTerm.setId(UUID.randomUUID());
		}
		return paymentTermRepositoryPort.save(paymentTerm);
	}

	static void validateInstallments(PaymentTermDomain paymentTerm) {
		if (paymentTerm.getInstallmentIntervalsDays() == null || paymentTerm.getInstallmentIntervalsDays().isEmpty()) {
			throw new BusinessRuleException("A payment term needs at least one installment interval");
		}
	}
}
