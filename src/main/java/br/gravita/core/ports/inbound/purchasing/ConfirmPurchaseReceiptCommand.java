package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.Objects;

public record ConfirmPurchaseReceiptCommand(PurchaseReceiptId receiptId) {

	public ConfirmPurchaseReceiptCommand {
		Objects.requireNonNull(receiptId, "receiptId is required");
	}
}
