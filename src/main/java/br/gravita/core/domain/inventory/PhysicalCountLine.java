package br.gravita.core.domain.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A product's system on-hand quantity, captured at count-start time
 * (UC-M5-08, AC3) so later stock movements don't retroactively change what
 * gets compared against the physical count.
 */
public record PhysicalCountLine(UUID productId, BigDecimal systemQuantity) {

	public PhysicalCountLine {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(systemQuantity, "systemQuantity is required");
	}
}
