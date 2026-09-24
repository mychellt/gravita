package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.*;

import java.util.List;
import java.util.UUID;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public final class PurchaseRequest {

    private PurchaseRequestId id;
    private PurchaseRequestOrigin origin;
    private List<PurchaseRequestItem> items;
    private UUID requestedBy;
    private PurchaseRequestStatus status;

    public static PurchaseRequest open(PurchaseRequestId id, PurchaseRequestOrigin origin,
                                       List<PurchaseRequestItem> items, UUID requestedBy) {
        return new PurchaseRequest(id, origin, items, requestedBy, PurchaseRequestStatus.OPEN);
    }

    public static PurchaseRequest of(PurchaseRequestId id, PurchaseRequestOrigin origin,
                                     List<PurchaseRequestItem> items, UUID requestedBy, PurchaseRequestStatus status) {
        return new PurchaseRequest(id, origin, items, requestedBy, status);
    }

    public PurchaseRequest quote() {
        if (status != PurchaseRequestStatus.OPEN) {
            throw new BusinessRuleException(
                    "Only a request in status OPEN can be sent for quotation, was " + status);
        }
        return new PurchaseRequest(id, origin, items, requestedBy, PurchaseRequestStatus.QUOTED);
    }

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
