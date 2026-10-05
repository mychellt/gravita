package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RenegotiationResponse(UUID id, UUID customerId, List<UUID> originalReceivableIds,
		List<UUID> newReceivableIds, Instant createdAt) {

	public static RenegotiationResponse from(final Renegotiation renegotiation) {
		return new RenegotiationResponse(renegotiation.getId().value(), renegotiation.getCustomerId(),
				renegotiation.getOriginalReceivableIds().stream().map(ReceivableId::value).toList(),
				renegotiation.getNewReceivableIds().stream().map(ReceivableId::value).toList(),
				renegotiation.getCreatedAt());
	}
}
