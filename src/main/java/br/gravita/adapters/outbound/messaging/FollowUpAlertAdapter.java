package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FollowUpAlertAdapter implements SendFollowUpAlertPort {

	private static final Logger log = LoggerFactory.getLogger(FollowUpAlertAdapter.class);

	private final EmailNotificationPort emailNotificationPort;
	private final String recipientEmail;

	public FollowUpAlertAdapter(EmailNotificationPort emailNotificationPort,
			@Value("${notifications.crm.follow-up-email}") String recipientEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.recipientEmail = recipientEmail;
	}

	@Override
	public void send(FollowUpTask task) {
		if (task.getAlertChannel() == AlertChannel.EMAIL) {
			emailNotificationPort.send(recipientEmail, subject(task), body(task));
		} else {
			// No in-app notification module exists yet; log so the alert isn't silently dropped.
			log.info("Follow-up task app alert for owner {}: {}", task.getOwner(), body(task));
		}
	}

	private String subject(FollowUpTask task) {
		return "Follow-up task due " + task.getDueDate();
	}

	private String body(FollowUpTask task) {
		return "Follow-up task " + task.getId().value() + " for owner " + task.getOwner() + " is due "
				+ task.getDueDate() + describeLink(task);
	}

	private String describeLink(FollowUpTask task) {
		if (task.getOpportunityId() != null) {
			return " (opportunity " + task.getOpportunityId() + ")";
		}
		return " (customer " + task.getCustomerId() + ")";
	}
}
