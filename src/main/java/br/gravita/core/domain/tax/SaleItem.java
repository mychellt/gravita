package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record SaleItem(UUID productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal itemDiscount) {

	public SaleItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
		itemDiscount = itemDiscount == null ? BigDecimal.ZERO : itemDiscount;
		if (itemDiscount.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item discount cannot be negative: " + itemDiscount);
		}
		if (itemDiscount.compareTo(subtotal(quantity, unitPrice)) > 0) {
			throw new BusinessRuleException("Item discount (" + itemDiscount + ") cannot exceed the item subtotal");
		}
	}

	public BigDecimal subtotal() {
		return subtotal(quantity, unitPrice);
	}

	public BigDecimal lineTotal() {
		return subtotal().subtract(itemDiscount);
	}

	private static BigDecimal subtotal(final BigDecimal quantity, final BigDecimal unitPrice) {
		return unitPrice.multiply(quantity);
	}
}
