package br.gravita.core.domain.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
