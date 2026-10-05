package br.gravita.adapters.outbound.messaging;

import br.gravita.core.ports.inbound.inventory.ExpiringLotView;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.inventory.NotifyExpiringLotPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExpiringLotNotificationAdapter implements NotifyExpiringLotPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String recipientEmail;

	public ExpiringLotNotificationAdapter(final EmailNotificationPort emailNotificationPort,
			@Value("${notifications.inventory.expiring-lots-email}") final String recipientEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.recipientEmail = recipientEmail;
	}

	@Override
	public void notify(final List<ExpiringLotView> expiringLots) {
		if (expiringLots.isEmpty()) {
			return;
		}

		final String subject = expiringLots.size() + " lot(s) approaching expiry";
		final String body = expiringLots.stream().map(this::describe).collect(Collectors.joining("\n"));
		emailNotificationPort.send(recipientEmail, subject, body);
	}

	private String describe(final ExpiringLotView lot) {
		return "Product " + lot.productId() + ", lot " + lot.lotCode() + ", expires " + lot.expiryDate()
				+ ", remaining quantity " + lot.remainingQuantity();
	}
}
