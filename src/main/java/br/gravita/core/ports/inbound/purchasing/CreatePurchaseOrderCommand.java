package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code quotationId} is optional - null for a request converted without a
 * formal quotation. Unit prices always come from {@code items} directly,
 * whether or not they were informed by a quotation comparison (see
 * {@link br.gravita.core.domain.purchasing.PurchaseOrder}).
 */
public record CreatePurchaseOrderCommand(
		PurchaseRequestId requestId,
		UUID quotationId,
		SupplierId supplierId,
		List<PurchaseOrderItem> items) {

	public CreatePurchaseOrderCommand {
		Objects.requireNonNull(requestId, "requestId is required");
		Objects.requireNonNull(supplierId, "supplierId is required");
		items = items == null ? List.of() : List.copyOf(items);
	}
}
