package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ReturnToSupplierCommand(PurchaseReceiptId receiptId, List<ReturnedItem> items) {

	public ReturnToSupplierCommand {
		Objects.requireNonNull(receiptId, "receiptId is required");
		items = items == null ? List.of() : List.copyOf(items);
	}

	public record ReturnedItem(UUID productId, BigDecimal quantity) {
		public ReturnedItem {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
		}
	}
}
