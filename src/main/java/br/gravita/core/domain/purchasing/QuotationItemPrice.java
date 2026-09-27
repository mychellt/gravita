package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record QuotationItemPrice(UUID productId, BigDecimal unitPrice) {

	public QuotationItemPrice {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
	}
}
