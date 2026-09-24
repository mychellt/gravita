package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Document-level totals parsed from a supplier's NFe XML ({@code total/ICMSTot}).
 */
public record InboundNfeTotals(
		BigDecimal productsValue,
		BigDecimal freightValue,
		BigDecimal insuranceValue,
		BigDecimal discountValue,
		BigDecimal otherExpensesValue,
		BigDecimal icmsValue,
		BigDecimal ipiValue,
		BigDecimal pisValue,
		BigDecimal cofinsValue,
		BigDecimal totalValue) {

	public InboundNfeTotals {
		Objects.requireNonNull(productsValue, "productsValue is required");
		Objects.requireNonNull(totalValue, "totalValue is required");
		freightValue = zeroIfNull(freightValue);
		insuranceValue = zeroIfNull(insuranceValue);
		discountValue = zeroIfNull(discountValue);
		otherExpensesValue = zeroIfNull(otherExpensesValue);
		icmsValue = zeroIfNull(icmsValue);
		ipiValue = zeroIfNull(ipiValue);
		pisValue = zeroIfNull(pisValue);
		cofinsValue = zeroIfNull(cofinsValue);
		if (productsValue.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("productsValue cannot be negative: " + productsValue);
		}
		if (totalValue.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("totalValue cannot be negative: " + totalValue);
		}
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
