package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.ports.business.SetCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bridges {@code finance} to M1's {@link SetCustomerCreditStatusPort}. The
 * customer's balance is what is still owed on their unsettled titles and the
 * status follows M1's rule: {@code BLOCKED} once the balance exceeds the credit
 * limit (a missing or zero limit means none is enforced), otherwise {@code DELINQUENT} while any unsettled
 * title is past its due date, otherwise {@code REGULAR}.
 * <p>
 * The baixa that triggers this is already recorded, so a failure to refresh the
 * status must never undo it: it is logged at ERROR and the next change to the
 * customer's titles recomputes the whole position anyway.
 */
@Component
class UpdateCustomerCreditStatusAdapter implements UpdateCustomerCreditStatusPort {

	private static final Logger log = LoggerFactory.getLogger(UpdateCustomerCreditStatusAdapter.class);

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final SetCustomerCreditStatusPort setCustomerCreditStatusPort;

	UpdateCustomerCreditStatusAdapter(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort, CustomerRepositoryPort customerRepositoryPort,
			SetCustomerCreditStatusPort setCustomerCreditStatusPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.setCustomerCreditStatusPort = setCustomerCreditStatusPort;
	}

	@Override
	public void update(UUID customerId) {
		try {
			Optional<CustomerDomain> customer = customerRepositoryPort.get(customerId);
			if (customer.isEmpty()) {
				log.warn("Customer {} not found; credit status not updated", customerId);
				return;
			}
			List<Receivable> unsettled = receivableRepositoryPort.findUnsettledByCustomerId(customerId);
			BigDecimal balance = unsettled.stream()
					.map(receivable -> receivable
							.remainingBalance(settlementRepositoryPort.findByReceivableId(receivable.getId())))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			LocalDate today = LocalDate.now();
			boolean overdue = unsettled.stream().anyMatch(receivable -> receivable.getDueDate().isBefore(today));

			CustomerDomain position = CustomerDomain.builder().id(customerId).currentBalance(balance)
					.status(statusFor(customer.get().getCreditLimit(), balance, overdue)).build();
			setCustomerCreditStatusPort.execute(new Context(position));
		} catch (RuntimeException e) {
			log.error("Failed to update credit status of customer {}; needs retry", customerId, e);
		}
	}

	private static CustomerStatus statusFor(BigDecimal creditLimit, BigDecimal balance, boolean overdue) {
		if (creditLimit != null && creditLimit.signum() > 0 && balance.compareTo(creditLimit) > 0) {
			return CustomerStatus.BLOCKED;
		}
		return overdue ? CustomerStatus.DELINQUENT : CustomerStatus.REGULAR;
	}
}
