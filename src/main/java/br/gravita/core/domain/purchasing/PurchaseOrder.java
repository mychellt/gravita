package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@AllArgsConstructor
@Builder
@Getter
public final class PurchaseOrder {

    private PurchaseOrderId id;
    private PurchaseRequestId requestId;
    private UUID quotationId;
    private SupplierId supplierId;
    private List<PurchaseOrderItem> items;
    private boolean approvalRequired;
    private PurchaseOrderStatus status;
    private UUID approvedBy;

    public static PurchaseOrder create(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId,
                                       SupplierId supplierId, List<PurchaseOrderItem> items, boolean approvalRequired) {
        return new PurchaseOrder(id, requestId, quotationId, supplierId, requireNonEmptyItems(items), approvalRequired,
                PurchaseOrderStatus.OPEN, null);
    }

    public static PurchaseOrder of(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId,
                                   SupplierId supplierId, List<PurchaseOrderItem> items, boolean approvalRequired,
                                   PurchaseOrderStatus status) {
        return of(id, requestId, quotationId, supplierId, items, approvalRequired, status, null);
    }

    public static PurchaseOrder of(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId,
                                   SupplierId supplierId, List<PurchaseOrderItem> items, boolean approvalRequired,
                                   PurchaseOrderStatus status, UUID approvedBy) {
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired, status, approvedBy);
    }

    public void assertReceivable() {
        if (status != PurchaseOrderStatus.OPEN && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException(
                    "A receipt can only be opened against an OPEN or PARTIALLY_RECEIVED order, was " + status);
        }
        if (approvalRequired) {
            throw new BusinessRuleException("Cannot receive an order pending approval (UC-M6-05)");
        }
    }

    public PurchaseOrder approve(UUID approvedBy) {
        assertPendingApproval("approve");
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, false, status, approvedBy);
    }

    public PurchaseOrder reject() {
        assertPendingApproval("reject");
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired,
                PurchaseOrderStatus.CANCELLED, approvedBy);
    }

    private void assertPendingApproval(String action) {
        if (!approvalRequired) {
            throw new BusinessRuleException(
                    "Cannot " + action + " an order that does not require approval (UC-M6-05)");
        }
        if (status != PurchaseOrderStatus.OPEN) {
            throw new BusinessRuleException("Cannot " + action + " an order in " + status + " state (UC-M6-05)");
        }
    }

    public PurchaseOrder afterReceiptConfirmed(boolean fullyReceived) {
        if (status == PurchaseOrderStatus.CLOSED || status == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot confirm a receipt against a " + status + " order");
        }
        PurchaseOrderStatus newStatus = fullyReceived ? PurchaseOrderStatus.CLOSED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED;
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired, newStatus, approvedBy);
    }

    public BigDecimal totalValue() {
        return totalValue(items);
    }

    public static BigDecimal totalValue(List<PurchaseOrderItem> items) {
        return items.stream().map(PurchaseOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<PurchaseOrderItem> requireNonEmptyItems(List<PurchaseOrderItem> items) {
        List<PurchaseOrderItem> copy = items == null ? List.of() : List.copyOf(items);
        if (copy.isEmpty()) {
            throw new BusinessRuleException("A purchase order must have at least one item");
        }
        return copy;
    }
}
