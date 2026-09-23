package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrderId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ReceivePurchaseOrderCommand(PurchaseOrderId orderId, List<ReceivedItem> receivedItems) {

	public ReceivePurchaseOrderCommand {
		Objects.requireNonNull(orderId, "orderId is required");
		receivedItems = receivedItems == null ? List.of() : List.copyOf(receivedItems);
	}

	public record ReceivedItem(UUID productId, BigDecimal receivedQty) {
		public ReceivedItem {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(receivedQty, "receivedQty is required");
		}
	}
}
