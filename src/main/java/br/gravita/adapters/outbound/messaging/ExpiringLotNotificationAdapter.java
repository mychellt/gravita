package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.inbound.inventory.ExpiringLotView;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.inventory.NotifyExpiringLotPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivers UC-M5-10's expiring-lot alert over the existing e-mail channel
 * ({@link EmailNotificationPort}), the same stand-in used for the purchasing
 * approval-workflow notification until M9/M10 land their own dashboard/alerting
 * consumers. An empty result is not notified - there is nothing for the recipient
 * to act on.
 */
@Component
public class ExpiringLotNotificationAdapter implements NotifyExpiringLotPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String recipientEmail;

	public ExpiringLotNotificationAdapter(EmailNotificationPort emailNotificationPort,
			@Value("${notifications.inventory.expiring-lots-email}") String recipientEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.recipientEmail = recipientEmail;
	}

	@Override
	public void notify(List<ExpiringLotView> expiringLots) {
		if (expiringLots.isEmpty()) {
			return;
		}

		String subject = expiringLots.size() + " lot(s) approaching expiry";
		String body = expiringLots.stream().map(this::describe).collect(Collectors.joining("\n"));
		emailNotificationPort.send(recipientEmail, subject, body);
	}

	private String describe(ExpiringLotView lot) {
		return "Product " + lot.productId() + ", lot " + lot.lotCode() + ", expires " + lot.expiryDate()
				+ ", remaining quantity " + lot.remainingQuantity();
	}
}
