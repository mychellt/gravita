package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.business.ListCustomersPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCustomersAdapter implements ListCustomersPort {

	private final CustomerRepositoryPort customerRepositoryPort;

	public ListCustomersAdapter(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public List<CustomerDomain> execute(Context context) {
		return customerRepositoryPort.findAll();
	}
}
