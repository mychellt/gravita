package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PurchaseReceiptItem(UUID productId, BigDecimal orderedQty, BigDecimal receivedQty) {

	public PurchaseReceiptItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(orderedQty, "orderedQty is required");
		Objects.requireNonNull(receivedQty, "receivedQty is required");
		if (orderedQty.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item orderedQty must be positive: " + orderedQty);
		}
		if (receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item receivedQty must be positive: " + receivedQty);
		}
	}
}
