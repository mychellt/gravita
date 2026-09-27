package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A returned line, keyed by {@code productOrServiceId} like
 * {@code purchasing.PurchaseReturnItem} - a {@link SalesOrderItem} has no
 * separate line id to reference instead.
 */
public record SalesReturnItem(UUID productOrServiceId, BigDecimal quantity) {

	public SalesReturnItem {
		Objects.requireNonNull(productOrServiceId, "productOrServiceId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Return item quantity must be positive: " + quantity);
		}
	}
}
