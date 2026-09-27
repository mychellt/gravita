package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * An order line. {@code discount} is an absolute amount taken off the line subtotal
 * ({@code quantity × unitPrice}), mirroring {@code QuoteItem.discount}.
 */
public record SalesOrderItem(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitPrice,
		BigDecimal discount) {

	public SalesOrderItem {
		Objects.requireNonNull(productOrServiceId, "productOrServiceId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		discount = discount == null ? BigDecimal.ZERO : discount;
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
		if (discount.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item discount cannot be negative: " + discount);
		}
		if (discount.compareTo(quantity.multiply(unitPrice)) > 0) {
			throw new BusinessRuleException("Item discount (" + discount + ") cannot exceed the item subtotal");
		}
	}

	public static SalesOrderItem fromQuoteItem(QuoteItem item) {
		return new SalesOrderItem(item.productOrServiceId(), item.quantity(), item.unitPrice(), item.discount());
	}

	public BigDecimal subtotal() {
		return quantity.multiply(unitPrice);
	}

	public BigDecimal lineTotal() {
		return subtotal().subtract(discount);
	}
}
