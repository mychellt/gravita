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
	private final CallerCompanyResolver callerCompanyResolver;

	public ListCustomersAdapter(final CustomerRepositoryPort customerRepositoryPort, final CallerCompanyResolver callerCompanyResolver) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<CustomerDomain> execute(final Context context) {
		return callerCompanyResolver.resolve(context)
				.map(customerRepositoryPort::findAllByCompanyId)
				.orElse(List.of());
	}
}
