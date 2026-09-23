package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ApprovePurchaseOrderCommand(PurchaseOrderId orderId, UUID approvedBy, ApprovalDecision decision) {
}
