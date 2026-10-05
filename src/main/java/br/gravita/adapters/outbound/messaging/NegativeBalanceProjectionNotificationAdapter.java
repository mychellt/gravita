package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.finance.NegativeBalanceProjectionAlert;
import br.gravita.core.ports.outbound.finance.NotifyNegativeBalanceProjectionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NegativeBalanceProjectionNotificationAdapter implements NotifyNegativeBalanceProjectionPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String recipientEmail;

	public NegativeBalanceProjectionNotificationAdapter(final EmailNotificationPort emailNotificationPort,
			@Value("${notifications.finance.cash-flow-alert-email:}") final String recipientEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.recipientEmail = recipientEmail;
	}

	@Override
	public void notify(final NegativeBalanceProjectionAlert alert) {
		if (recipientEmail == null || recipientEmail.isBlank()) {
			log.warn("Projected cash balance goes negative from {} (lowest {}) but no recipient is configured",
					alert.firstNegativePeriodStart(), alert.lowestBalance());
			return;
		}
		final String subject = "Projected cash balance goes negative from " + alert.firstNegativePeriodStart();
		final String body = "The " + alert.granularity().name().toLowerCase() + " cash-flow projection closes negative from "
				+ alert.firstNegativePeriodStart() + "; the lowest projected balance is " + alert.lowestBalance()
				+ ".\n" + describe(alert.filter());
		emailNotificationPort.send(recipientEmail, subject, body);
	}

	private String describe(final CashFlowFilter filter) {
		return "Company: " + orAll(filter.companyId()) + ", branch: " + orAll(filter.branchId()) + ", bank account: "
				+ orAll(filter.bankAccountId()) + ", cost center: " + orAll(filter.costCenterId());
	}

	private static String orAll(final Object value) {
		return value == null ? "all" : value.toString();
	}
}
