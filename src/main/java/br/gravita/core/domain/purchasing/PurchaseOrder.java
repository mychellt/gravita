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

    public static PurchaseOrder create(final PurchaseOrderId id, final PurchaseRequestId requestId, final UUID quotationId,
                                       final SupplierId supplierId, final List<PurchaseOrderItem> items, final boolean approvalRequired) {
        return new PurchaseOrder(id, requestId, quotationId, supplierId, requireNonEmptyItems(items), approvalRequired,
                PurchaseOrderStatus.OPEN, null);
    }

    public static PurchaseOrder of(final PurchaseOrderId id, final PurchaseRequestId requestId, final UUID quotationId,
                                   final SupplierId supplierId, final List<PurchaseOrderItem> items, final boolean approvalRequired,
                                   final PurchaseOrderStatus status) {
        return of(id, requestId, quotationId, supplierId, items, approvalRequired, status, null);
    }

    public static PurchaseOrder of(final PurchaseOrderId id, final PurchaseRequestId requestId, final UUID quotationId,
                                   final SupplierId supplierId, final List<PurchaseOrderItem> items, final boolean approvalRequired,
                                   final PurchaseOrderStatus status, final UUID approvedBy) {
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

    public PurchaseOrder approve(final UUID approvedBy) {
        assertPendingApproval("approve");
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, false, status, approvedBy);
    }

    public PurchaseOrder reject() {
        assertPendingApproval("reject");
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired,
                PurchaseOrderStatus.CANCELLED, approvedBy);
    }

    private void assertPendingApproval(final String action) {
        if (!approvalRequired) {
            throw new BusinessRuleException(
                    "Cannot " + action + " an order that does not require approval (UC-M6-05)");
        }
        if (status != PurchaseOrderStatus.OPEN) {
            throw new BusinessRuleException("Cannot " + action + " an order in " + status + " state (UC-M6-05)");
        }
    }

    public PurchaseOrder afterReceiptConfirmed(final boolean fullyReceived) {
        if (status == PurchaseOrderStatus.CLOSED || status == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot confirm a receipt against a " + status + " order");
        }
        final PurchaseOrderStatus newStatus = fullyReceived ? PurchaseOrderStatus.CLOSED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED;
        return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired, newStatus, approvedBy);
    }

    public BigDecimal totalValue() {
        return totalValue(items);
    }

    public static BigDecimal totalValue(final List<PurchaseOrderItem> items) {
        return items.stream().map(PurchaseOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<PurchaseOrderItem> requireNonEmptyItems(final List<PurchaseOrderItem> items) {
        final List<PurchaseOrderItem> copy = items == null ? List.of() : List.copyOf(items);
        if (copy.isEmpty()) {
            throw new BusinessRuleException("A purchase order must have at least one item");
        }
        return copy;
    }
}
