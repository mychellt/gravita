package br.gravita.adapters.outbound.messaging;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.purchasing.NotifyApprovalWorkflowPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Delivers UC-M6-05's approval-workflow notification over the existing e-mail channel
 * ({@link EmailNotificationPort}). Recipient resolution is config-driven (like {@code
 * aws.sqs.email-queue-url}) rather than looked up per-order, since no domain link from an
 * {@code ApprovalAlcada.approverProfileId} to a deliverable address exists yet.
 */
@Component
public class ApprovalWorkflowNotificationAdapter implements NotifyApprovalWorkflowPort {

	private final EmailNotificationPort emailNotificationPort;
	private final String approverEmail;

	public ApprovalWorkflowNotificationAdapter(EmailNotificationPort emailNotificationPort,
			@Value("${notifications.purchasing.approver-email}") String approverEmail) {
		this.emailNotificationPort = emailNotificationPort;
		this.approverEmail = approverEmail;
	}

	@Override
	public void notifyDecision(PurchaseOrder order, ApprovalDecision decision) {
		String verb = decision == ApprovalDecision.APPROVE ? "approved" : "rejected";
		String subject = "Purchase order " + order.getId().value() + " " + verb;
		String body = decision == ApprovalDecision.APPROVE
				? "Purchase order " + order.getId().value() + " was approved by " + order.getApprovedBy() + "."
				: "Purchase order " + order.getId().value() + " was rejected and cancelled.";
		emailNotificationPort.send(approverEmail, subject, body);
	}
}
