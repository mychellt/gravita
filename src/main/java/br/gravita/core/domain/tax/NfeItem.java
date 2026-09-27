package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A line of an {@link NfeDocument}. {@code taxBreakdown} always comes from
 * {@link br.gravita.core.ports.inbound.tax.CalculateTaxUseCase} (UC-M2-01,
 * AC4); a manual override of any of its tax lines only reaches here already
 * carrying its justification (see {@link TaxLineBreakdown#overridden()}).
 */
public record NfeItem(UUID productId, String description, BigDecimal quantity, BigDecimal unitPrice,
		BigDecimal discount, ItemTaxBreakdown taxBreakdown) {

	public NfeItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		Objects.requireNonNull(taxBreakdown, "taxBreakdown is required");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
		discount = discount == null ? BigDecimal.ZERO : discount;
		if (discount.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item discount cannot be negative: " + discount);
		}
		if (discount.compareTo(subtotal(quantity, unitPrice)) > 0) {
			throw new BusinessRuleException("Item discount (" + discount + ") cannot exceed the item subtotal");
		}
	}

	public BigDecimal subtotal() {
		return subtotal(quantity, unitPrice);
	}

	public BigDecimal lineTotal() {
		return subtotal().subtract(discount);
	}

	private static BigDecimal subtotal(BigDecimal quantity, BigDecimal unitPrice) {
		return unitPrice.multiply(quantity);
	}
}
