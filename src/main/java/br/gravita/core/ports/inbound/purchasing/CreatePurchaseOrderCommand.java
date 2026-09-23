package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreatePurchaseOrderCommand(
        PurchaseRequestId requestId,
        UUID quotationId,
        SupplierId supplierId,
        List<PurchaseOrderItem> items) {
}
