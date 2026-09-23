package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * PurchaseOrder aggregate (UC-M6-04). Converts a quoted - or, with no formal
 * quotation, a directly requested - {@link PurchaseRequest} into an order
 * against one chosen supplier. {@code quotationId} stays a bare, unvalidated
 * reference until `SendQuotationUseCase`/`RegisterQuotationResponseUseCase`
 * (M6-02/03) land a real `Quotation` aggregate to check it against; the
 * order's prices are always taken from the command's items directly, whether
 * or not a quotation informed them. A newly created order is always
 * {@link PurchaseOrderStatus#OPEN} - later use cases in this module drive it
 * through the rest of the lifecycle.
 */
@Getter
public final class PurchaseOrder {

	private final PurchaseOrderId id;
	private final PurchaseRequestId requestId;
	private final UUID quotationId;
	private final SupplierId supplierId;
	private final List<PurchaseOrderItem> items;
	private final boolean approvalRequired;
	private final PurchaseOrderStatus status;

	private PurchaseOrder(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId, SupplierId supplierId,
			List<PurchaseOrderItem> items, boolean approvalRequired, PurchaseOrderStatus status) {
		this.id = Objects.requireNonNull(id, "PurchaseOrderId is required");
		this.requestId = Objects.requireNonNull(requestId, "requestId is required");
		this.quotationId = quotationId;
		this.supplierId = Objects.requireNonNull(supplierId, "supplierId is required");
		this.items = requireNonEmptyItems(items);
		this.approvalRequired = approvalRequired;
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static PurchaseOrder create(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId,
			SupplierId supplierId, List<PurchaseOrderItem> items, boolean approvalRequired) {
		return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired,
				PurchaseOrderStatus.OPEN);
	}

	public static PurchaseOrder of(PurchaseOrderId id, PurchaseRequestId requestId, UUID quotationId,
			SupplierId supplierId, List<PurchaseOrderItem> items, boolean approvalRequired,
			PurchaseOrderStatus status) {
		return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired, status);
	}

	/**
	 * UC-M6-06: an order can only be received while still open for receiving
	 * and, when it required approval, only once approved. There is no way yet
	 * to represent "approved" - {@code ApprovePurchaseOrderUseCase} (UC-M6-05)
	 * hasn't landed - so for now an order flagged {@code approvalRequired}
	 * simply can't be received at all until that use case exists.
	 */
	public void assertReceivable() {
		if (status != PurchaseOrderStatus.OPEN && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
			throw new BusinessRuleException(
					"A receipt can only be opened against an OPEN or PARTIALLY_RECEIVED order, was " + status);
		}
		if (approvalRequired) {
			throw new BusinessRuleException("Cannot receive an order pending approval (UC-M6-05)");
		}
	}

	/**
	 * UC-M6-08: applies the outcome of a confirmed receipt. The order closes
	 * once every item's ordered quantity has been covered across all of its
	 * confirmed receipts; otherwise it (or stays) {@code PARTIALLY_RECEIVED}.
	 */
	public PurchaseOrder afterReceiptConfirmed(boolean fullyReceived) {
		if (status == PurchaseOrderStatus.CLOSED || status == PurchaseOrderStatus.CANCELLED) {
			throw new BusinessRuleException("Cannot confirm a receipt against a " + status + " order");
		}
		PurchaseOrderStatus newStatus = fullyReceived ? PurchaseOrderStatus.CLOSED
				: PurchaseOrderStatus.PARTIALLY_RECEIVED;
		return new PurchaseOrder(id, requestId, quotationId, supplierId, items, approvalRequired, newStatus);
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
