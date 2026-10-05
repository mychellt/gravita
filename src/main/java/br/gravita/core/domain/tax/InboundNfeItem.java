package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;

public record InboundNfeItem(
		String supplierProductCode,
		String description,
		String ncm,
		String cfop,
		String unit,
		BigDecimal quantity,
		BigDecimal unitValue,
		BigDecimal totalValue,
		BigDecimal icmsValue,
		BigDecimal ipiValue,
		BigDecimal pisValue,
		BigDecimal cofinsValue) {

	public InboundNfeItem {
		if (supplierProductCode == null || supplierProductCode.isBlank()) {
			throw new BusinessRuleException("Item supplierProductCode is required");
		}
		if (description == null || description.isBlank()) {
			throw new BusinessRuleException("Item description is required");
		}
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitValue, "unitValue is required");
		Objects.requireNonNull(totalValue, "totalValue is required");
		icmsValue = requireNonNegative(icmsValue, "icmsValue");
		ipiValue = requireNonNegative(ipiValue, "ipiValue");
		pisValue = requireNonNegative(pisValue, "pisValue");
		cofinsValue = requireNonNegative(cofinsValue, "cofinsValue");
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitValue.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitValue cannot be negative: " + unitValue);
		}
		if (totalValue.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item totalValue cannot be negative: " + totalValue);
		}
	}

	private static BigDecimal requireNonNegative(final BigDecimal value, final String field) {
		final BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
		if (resolved.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item " + field + " cannot be negative: " + resolved);
		}
		return resolved;
	}
}
