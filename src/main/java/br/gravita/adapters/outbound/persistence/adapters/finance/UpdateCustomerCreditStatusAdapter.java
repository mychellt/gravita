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
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
class UpdateCustomerCreditStatusAdapter implements UpdateCustomerCreditStatusPort {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final SetCustomerCreditStatusPort setCustomerCreditStatusPort;

	UpdateCustomerCreditStatusAdapter(final ReceivableRepositoryPort receivableRepositoryPort,
			final SettlementRepositoryPort settlementRepositoryPort, final CustomerRepositoryPort customerRepositoryPort,
			final SetCustomerCreditStatusPort setCustomerCreditStatusPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.setCustomerCreditStatusPort = setCustomerCreditStatusPort;
	}

	@Override
	public void update(final UUID customerId) {
		try {
			final Optional<CustomerDomain> customer = customerRepositoryPort.get(customerId);
			if (customer.isEmpty()) {
				log.warn("Customer {} not found; credit status not updated", customerId);
				return;
			}
			final List<Receivable> unsettled = receivableRepositoryPort.findUnsettledByCustomerId(customerId);
			final BigDecimal balance = unsettled.stream()
					.map(receivable -> receivable
							.remainingBalance(settlementRepositoryPort.findByReceivableId(receivable.getId())))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			final LocalDate today = LocalDate.now();
			final boolean overdue = unsettled.stream().anyMatch(receivable -> receivable.getDueDate().isBefore(today));

			final CustomerDomain position = CustomerDomain.builder().id(customerId).currentBalance(balance)
					.status(statusFor(customer.get().getCreditLimit(), balance, overdue)).build();
			setCustomerCreditStatusPort.execute(new Context(position));
		} catch (final RuntimeException e) {
			log.error("Failed to update credit status of customer {}; needs retry", customerId, e);
		}
	}

	private static CustomerStatus statusFor(final BigDecimal creditLimit, final BigDecimal balance, final boolean overdue) {
		if (creditLimit != null && creditLimit.signum() > 0 && balance.compareTo(creditLimit) > 0) {
			return CustomerStatus.BLOCKED;
		}
		return overdue ? CustomerStatus.DELINQUENT : CustomerStatus.REGULAR;
	}
}
