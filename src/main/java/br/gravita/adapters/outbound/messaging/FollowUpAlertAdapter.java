package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FollowUpAlertAdapter implements SendFollowUpAlertPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String recipientEmail;

	public FollowUpAlertAdapter(final EmailNotificationPort emailNotificationPort,
			@Value("${notifications.crm.follow-up-email}") final String recipientEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.recipientEmail = recipientEmail;
	}

	@Override
	public void send(final FollowUpTask task) {
		if (task.getAlertChannel() == AlertChannel.EMAIL) {
			emailNotificationPort.send(recipientEmail, subject(task), body(task));
		} else {
			// No in-app notification module exists yet; log so the alert isn't silently dropped.
			log.info("Follow-up task app alert for owner {}: {}", task.getOwner(), body(task));
		}
	}

	private String subject(final FollowUpTask task) {
		return "Follow-up task due " + task.getDueDate();
	}

	private String body(final FollowUpTask task) {
		return "Follow-up task " + task.getId().value() + " for owner " + task.getOwner() + " is due "
				+ task.getDueDate() + describeLink(task);
	}

	private String describeLink(final FollowUpTask task) {
		if (task.getOpportunityId() != null) {
			return " (opportunity " + task.getOpportunityId() + ")";
		}
		return " (customer " + task.getCustomerId() + ")";
	}
}
