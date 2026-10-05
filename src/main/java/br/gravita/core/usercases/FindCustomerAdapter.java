package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.CustomerNotFoundException;
import br.gravita.core.ports.business.FindCustomerPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindCustomerAdapter implements FindCustomerPort {

	private final CustomerRepositoryPort customerRepositoryPort;

	public FindCustomerAdapter(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public CustomerDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		return customerRepositoryPort.get(id).orElseThrow(() -> new CustomerNotFoundException(id));
	}
}
