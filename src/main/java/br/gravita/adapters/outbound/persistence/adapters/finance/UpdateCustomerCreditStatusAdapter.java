package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.ports.business.SetCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridges {@code finance} to M1's {@link SetCustomerCreditStatusPort}. Finance
 * only derives {@code REGULAR} / {@code DELINQUENT} from the customer's titles,
 * so a customer that is {@code BLOCKED} keeps that status: a block is only
 * lifted explicitly, never as a side effect of a payment or renegotiation.
 */
@Component
class UpdateCustomerCreditStatusAdapter implements UpdateCustomerCreditStatusPort {

	private final SetCustomerCreditStatusPort setCustomerCreditStatusPort;
	private final CustomerRepositoryPort customerRepositoryPort;

	UpdateCustomerCreditStatusAdapter(SetCustomerCreditStatusPort setCustomerCreditStatusPort,
			CustomerRepositoryPort customerRepositoryPort) {
		this.setCustomerCreditStatusPort = setCustomerCreditStatusPort;
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public void update(UUID customerId, BigDecimal currentBalance, CustomerStatus status) {
		CustomerStatus effective = customerRepositoryPort.get(customerId).map(CustomerDomain::getStatus)
				.filter(CustomerStatus.BLOCKED::equals).orElse(status);
		setCustomerCreditStatusPort.execute(new Context(
				CustomerDomain.builder().id(customerId).currentBalance(currentBalance).status(effective).build()));
	}
}
