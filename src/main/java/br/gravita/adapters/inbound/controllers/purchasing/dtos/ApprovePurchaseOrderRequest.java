package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApprovePurchaseOrderRequest(@NotNull UUID approvedBy, @NotNull ApprovalDecision decision) {

	public ApprovePurchaseOrderCommand toCommand(final UUID orderId) {
		return new ApprovePurchaseOrderCommand(PurchaseOrderId.of(orderId), approvedBy, decision);
	}
}
