package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.SetCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class SetCustomerCreditStatusAdapter implements SetCustomerCreditStatusPort {

	private final CustomerRepositoryPort customerRepositoryPort;

	public SetCustomerCreditStatusAdapter(final CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public CustomerDomain execute(final Context context) {
		final CustomerDomain command = context.getData(CustomerDomain.class);
		final CustomerDomain customer = customerRepositoryPort.get(command.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + command.getId()));

		customer.applyCreditStatus(command.getCurrentBalance(), command.getStatus());

		return customerRepositoryPort.save(customer);
	}
}
