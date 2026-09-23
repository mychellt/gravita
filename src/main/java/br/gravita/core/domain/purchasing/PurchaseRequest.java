package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * PurchaseRequest aggregate (UC-M6-01). Opened by a user, by inventory's
 * min-stock trigger, or by sales-order demand; both trigger origins are
 * external callers of {@link #open} rather than separate use cases
 * (docs/specs/m6-compras/uc-01-create-purchase-request.md). A request is
 * always born {@link PurchaseRequestStatus#OPEN} - later use cases in this
 * module drive it through the rest of the lifecycle.
 */
@Getter
public final class PurchaseRequest {

	private final PurchaseRequestId id;
	private final PurchaseRequestOrigin origin;
	private final List<PurchaseRequestItem> items;
	private final UUID requestedBy;
	private final PurchaseRequestStatus status;

	private PurchaseRequest(PurchaseRequestId id, PurchaseRequestOrigin origin, List<PurchaseRequestItem> items,
			UUID requestedBy, PurchaseRequestStatus status) {
		this.id = Objects.requireNonNull(id, "PurchaseRequestId is required");
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.items = requireNonEmptyItems(items);
		this.requestedBy = requireConsistentRequestedBy(origin, requestedBy);
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static PurchaseRequest open(PurchaseRequestId id, PurchaseRequestOrigin origin,
			List<PurchaseRequestItem> items, UUID requestedBy) {
		return new PurchaseRequest(id, origin, items, requestedBy, PurchaseRequestStatus.OPEN);
	}

	public static PurchaseRequest of(PurchaseRequestId id, PurchaseRequestOrigin origin,
			List<PurchaseRequestItem> items, UUID requestedBy, PurchaseRequestStatus status) {
		return new PurchaseRequest(id, origin, items, requestedBy, status);
	}

	/**
	 * UC-M6-04: converts this request into a {@code PurchaseOrder}. Only a
	 * request still awaiting conversion ({@code OPEN} or {@code QUOTED}) can be
	 * converted.
	 */
	public PurchaseRequest convert() {
		if (status != PurchaseRequestStatus.OPEN && status != PurchaseRequestStatus.QUOTED) {
			throw new BusinessRuleException(
					"Only a request in status OPEN or QUOTED can be converted to a purchase order, was " + status);
		}
		return new PurchaseRequest(id, origin, items, requestedBy, PurchaseRequestStatus.CONVERTED);
	}

	private static List<PurchaseRequestItem> requireNonEmptyItems(List<PurchaseRequestItem> items) {
		List<PurchaseRequestItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A purchase request must have at least one item");
		}
		return copy;
	}

	private static UUID requireConsistentRequestedBy(PurchaseRequestOrigin origin, UUID requestedBy) {
		if (origin == PurchaseRequestOrigin.USER && requestedBy == null) {
			throw new BusinessRuleException("requestedBy is required when origin is USER");
		}
		if (origin != PurchaseRequestOrigin.USER && requestedBy != null) {
			throw new BusinessRuleException("requestedBy must be null for system-triggered origin " + origin);
		}
		return requestedBy;
	}
}
