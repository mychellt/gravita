package br.gravita.core.domain.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ConferenceResult(List<ConferenceLine> lines, BigDecimal orderedValue, BigDecimal invoicedValue) {

	public ConferenceResult {
		lines = lines == null ? List.of() : List.copyOf(lines);
		Objects.requireNonNull(orderedValue, "orderedValue is required");
		Objects.requireNonNull(invoicedValue, "invoicedValue is required");
	}

	public boolean hasDivergences() {
		return lines.stream().anyMatch(ConferenceLine::isDivergent) || orderedValue.compareTo(invoicedValue) != 0;
	}

	public record ConferenceLine(UUID productId, BigDecimal orderedQty, BigDecimal receivedQty) {

		public boolean isDivergent() {
			return orderedQty.compareTo(receivedQty) != 0;
		}
	}
}
