package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrder;
import java.time.LocalDateTime;

/** An order with the moment it was placed, which the order itself does not carry. */
public record PurchaseOrderRecord(PurchaseOrder order, LocalDateTime createdAt) {
}
