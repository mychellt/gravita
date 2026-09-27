package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.InteractionChannel;
import br.gravita.core.ports.inbound.sales.InteractionView;
import java.time.Instant;
import java.util.UUID;

public record InteractionResponse(UUID id, UUID opportunityId, UUID customerId, InteractionChannel channel,
		String summary, Instant timestamp) {

	public static InteractionResponse from(InteractionView view) {
		return new InteractionResponse(view.id(), view.opportunityId(), view.customerId(), view.channel(),
				view.summary(), view.timestamp());
	}
}
