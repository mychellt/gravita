package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One requested NFe line. {@code cfop} is deliberately not an input field
 * (AC2): the use case resolves it from the product's free CFOP registry.
 * {@code discountOverrideJustification} is required only when
 * {@code discountPercent} exceeds the linked price table's max-discount
 * rule (AC5); {@code taxOverrides} feeds {@code CalculateTaxUseCase}, which
 * already rejects an unjustified override (AC4).
 */
public record NfeItemInput(
		UUID productId,
		BigDecimal quantity,
		BigDecimal unitPrice,
		BigDecimal discountPercent,
		String discountOverrideJustification,
		List<NfeItemTaxOverrideInput> taxOverrides) {

	public NfeItemInput {
		Objects.requireNonNull(productId, "productId");
		Objects.requireNonNull(quantity, "quantity");
		Objects.requireNonNull(unitPrice, "unitPrice");
		discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
		taxOverrides = taxOverrides == null ? List.of() : List.copyOf(taxOverrides);
	}
}
