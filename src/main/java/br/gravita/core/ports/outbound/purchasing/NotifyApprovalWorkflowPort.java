package br.gravita.core.ports.outbound.purchasing;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrder;

public interface NotifyApprovalWorkflowPort {

	void notifyDecision(PurchaseOrder order, ApprovalDecision decision);
}
