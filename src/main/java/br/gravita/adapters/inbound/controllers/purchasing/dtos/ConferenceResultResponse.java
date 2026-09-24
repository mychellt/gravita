package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.ConferenceResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConferenceResultResponse(List<ConferenceLineResponse> lines, BigDecimal orderedValue,
		BigDecimal invoicedValue, boolean hasDivergences) {

	public static ConferenceResultResponse from(ConferenceResult result) {
		List<ConferenceLineResponse> lines = result.lines().stream()
				.map(line -> new ConferenceLineResponse(line.productId(), line.orderedQty(), line.receivedQty(),
						line.isDivergent()))
				.toList();
		return new ConferenceResultResponse(lines, result.orderedValue(), result.invoicedValue(),
				result.hasDivergences());
	}

	public record ConferenceLineResponse(UUID productId, BigDecimal orderedQty, BigDecimal receivedQty,
			boolean divergent) {
	}
}
