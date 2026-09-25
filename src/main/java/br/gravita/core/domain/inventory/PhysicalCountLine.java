package br.gravita.core.domain.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A product's system on-hand quantity, captured at count-start time
 * (UC-M5-08, AC3) so later stock movements don't retroactively change what
 * gets compared against the physical count. {@code countedQuantity} is
 * {@code null} until the count is submitted for approval (UC-M5-09); once
 * present, it's what {@link #hasDivergence()} and {@link #divergence()}
 * compare against {@code systemQuantity}.
 */
public record PhysicalCountLine(UUID productId, BigDecimal systemQuantity, BigDecimal countedQuantity) {

	public PhysicalCountLine {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(systemQuantity, "systemQuantity is required");
	}

	public PhysicalCountLine(UUID productId, BigDecimal systemQuantity) {
		this(productId, systemQuantity, null);
	}

	public boolean hasDivergence() {
		return countedQuantity != null && countedQuantity.compareTo(systemQuantity) != 0;
	}

	public BigDecimal divergence() {
		return countedQuantity.subtract(systemQuantity);
	}
}
