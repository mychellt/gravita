package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrder;
import java.time.LocalDateTime;

/** One row of the purchase orders list: the order, when it was placed and its supplier's name ({@code null} if unknown). */
public record PurchaseOrderListItem(PurchaseOrder order, LocalDateTime createdAt, String supplierName) {
}
