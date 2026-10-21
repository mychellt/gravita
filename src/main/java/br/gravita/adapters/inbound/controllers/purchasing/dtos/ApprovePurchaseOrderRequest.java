package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApprovePurchaseOrderRequest(UUID approvedBy, @NotNull ApprovalDecision decision) {

	/** The approver is {@code approvedBy} when the client names one, otherwise the logged user. */
	public ApprovePurchaseOrderCommand toCommand(final UUID orderId, final UUID loggedUser) {
		return new ApprovePurchaseOrderCommand(PurchaseOrderId.of(orderId), approvedBy != null ? approvedBy : loggedUser, decision);
	}
}
