package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * One NFe line (module spec: {@code NfeItem}). {@code cfop} is resolved by
 * the use case from the product's free CFOP registry (AC2) - never
 * hardcoded here - and {@code taxBreakdown} always comes from the shared
 * {@code CalculateTaxUseCase} (AC4), reused as-is rather than duplicated.
 */
public record NfeItem(
		UUID productId,
		BigDecimal quantity,
		BigDecimal unitPrice,
		BigDecimal discountPercent,
		String cfop,
		ItemTaxBreakdown taxBreakdown) {

	public NfeItem {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(taxBreakdown, "taxBreakdown is required");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Item quantity must be positive: " + quantity);
		}
		if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("Item unitPrice cannot be negative: " + unitPrice);
		}
		discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
		if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new BusinessRuleException("Item discountPercent must be between 0 and 100: " + discountPercent);
		}
		if (cfop == null || cfop.isBlank()) {
			throw new BusinessRuleException("Item cfop is required");
		}
	}

	public BigDecimal grossAmount() {
		return quantity.multiply(unitPrice);
	}

	public BigDecimal discountAmount() {
		return grossAmount().multiply(discountPercent).divide(BigDecimal.valueOf(100));
	}

	public BigDecimal netAmount() {
		return grossAmount().subtract(discountAmount());
	}
}
