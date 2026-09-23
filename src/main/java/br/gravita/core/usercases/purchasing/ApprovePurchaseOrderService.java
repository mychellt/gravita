package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.purchasing.NotifyApprovalWorkflowPort;

/**
 * UC-M6-05. Records the approver's decision against an order pending approval: approving
 * clears {@code approvalRequired} so UC-M6-06 (Receive) can start, rejecting cancels the
 * order outright.
 */
@UseCase
public class ApprovePurchaseOrderService implements ApprovePurchaseOrderUseCase {

	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final NotifyApprovalWorkflowPort notifyApprovalWorkflowPort;

	public ApprovePurchaseOrderService(PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			NotifyApprovalWorkflowPort notifyApprovalWorkflowPort) {
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.notifyApprovalWorkflowPort = notifyApprovalWorkflowPort;
	}

	@Override
	public void execute(ApprovePurchaseOrderCommand command) {
		PurchaseOrder order = purchaseOrderRepositoryPort.findById(command.orderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(command.orderId().value()));

		PurchaseOrder decided = command.decision() == ApprovalDecision.APPROVE
				? order.approve(command.approvedBy())
				: order.reject();

		purchaseOrderRepositoryPort.save(decided);
		notifyApprovalWorkflowPort.notifyDecision(decided, command.decision());
	}
}
