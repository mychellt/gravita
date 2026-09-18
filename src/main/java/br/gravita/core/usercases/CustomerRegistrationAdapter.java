package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import br.gravita.core.ports.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CustomerRegistrationAdapter implements CustomerRegistrationPort {

	private final CustomerRepositoryPort customerRepositoryPort;

	public CustomerRegistrationAdapter(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public CustomerDomain execute(Context context) {
		CustomerDomain customer = context.getData(CustomerDomain.class);
		customer.validateForRegistration();

		if (customer.getId() == null) {
			customer.setId(UUID.randomUUID());
		}
		if (customer.getCreditLimit() == null) {
			customer.setCreditLimit(BigDecimal.ZERO);
		}
		customer.setStatus(CustomerStatus.REGULAR);
		customer.setCurrentBalance(BigDecimal.ZERO);

		return customerRepositoryPort.save(customer);
	}
}
