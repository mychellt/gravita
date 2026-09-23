package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@code {product, quantity, unitPrice}} line on a {@link PurchaseOrder}.
 * {@code productId} references `masterdata`'s product by id rather than
 * embedding its domain type, mirroring {@link PurchaseRequestItem}.
 */
public record PurchaseOrderItem(UUID productId, BigDecimal quantity, BigDecimal unitPrice) {

	public PurchaseOrderItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
	}

	public BigDecimal lineTotal() {
		return unitPrice.multiply(quantity);
	}
}
