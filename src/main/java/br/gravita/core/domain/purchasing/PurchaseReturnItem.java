package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PurchaseReturnItem(UUID productId, BigDecimal quantity) {

	public PurchaseReturnItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
	}
}
