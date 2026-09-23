package br.gravita.core.ports.outbound.purchasing;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrder;

/**
 * Notifies the purchasing approval workflow (app/e-mail, doc UC-M6-05) once a decision has
 * been recorded against a {@link PurchaseOrder}.
 */
public interface NotifyApprovalWorkflowPort {

	void notifyDecision(PurchaseOrder order, ApprovalDecision decision);
}
