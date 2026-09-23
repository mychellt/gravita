package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@code {product, quantity}} line carried by a {@link Quotation} from its
 * originating {@link PurchaseRequest} (UC-M6-02), mirroring
 * {@link PurchaseRequestItem}. Used by UC-M6-03 to check that a supplier's
 * response prices every item.
 */
public record QuotationItem(UUID productId, BigDecimal quantity) {

	public QuotationItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
	}
}
