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
		BigDecimal cofinsValue,
		String barcode,
		String cest) {

	/** An item without GTIN/CEST (manual entry, or a supplier that does not state them). */
	public InboundNfeItem(final String supplierProductCode, final String description, final String ncm, final String cfop,
			final String unit, final BigDecimal quantity, final BigDecimal unitValue, final BigDecimal totalValue,
			final BigDecimal icmsValue, final BigDecimal ipiValue, final BigDecimal pisValue, final BigDecimal cofinsValue) {
		this(supplierProductCode, description, ncm, cfop, unit, quantity, unitValue, totalValue, icmsValue, ipiValue, pisValue,
				cofinsValue, null, null);
	}

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
		barcode = validGtin(barcode);
		cest = validCest(cest);
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

	/** The GTIN only when it is a real one: suppliers write "SEM GTIN" (or zeros) when the product has none. */
	private static String validGtin(final String barcode) {
		final String digits = barcode == null ? "" : barcode.trim();
		return digits.matches("\\d{8}|\\d{12,14}") && !digits.matches("0+") ? digits : null;
	}

	private static String validCest(final String cest) {
		final String digits = cest == null ? "" : cest.trim();
		return digits.matches("\\d{7}") ? digits : null;
	}
}
