package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CustomerRegistrationAdapter implements CustomerRegistrationPort {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public CustomerRegistrationAdapter(final CustomerRepositoryPort customerRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public CustomerDomain execute(final Context context) {
		final CustomerDomain customer = context.getData(CustomerDomain.class);
		customer.validateForRegistration();
		customer.setCompanyId(callerCompanyResolver.resolve(context)
				.orElseThrow(() -> new ForbiddenException("The caller does not belong to a company")));

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
