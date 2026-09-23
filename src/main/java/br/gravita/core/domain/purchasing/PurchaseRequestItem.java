package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@code {product, quantity}} line requested for purchase. {@code productId}
 * references `masterdata`'s product by id rather than embedding its domain
 * type, keeping purchasing's aggregate free of another context's model.
 */
public record PurchaseRequestItem(UUID productId, BigDecimal quantity) {

	public PurchaseRequestItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
	}
}
