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
	private final CallerCompanyResolver callerCompanyResolver;

	public FindCustomerAdapter(CustomerRepositoryPort customerRepositoryPort, CallerCompanyResolver callerCompanyResolver) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public CustomerDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		// A customer of another company is reported as not found, so its existence is not leaked.
		return callerCompanyResolver.resolve(context)
				.flatMap(companyId -> customerRepositoryPort.findByIdAndCompanyId(id, companyId))
				.orElseThrow(() -> new CustomerNotFoundException(id));
	}
}
