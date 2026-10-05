package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.InteractionChannel;
import java.time.Instant;
import java.util.UUID;

public record InteractionView(UUID id, UUID opportunityId, UUID customerId, InteractionChannel channel,
		String summary, Instant timestamp) {

	public static InteractionView from(final Interaction interaction) {
		return new InteractionView(
				interaction.getId().value(),
				interaction.getOpportunityId() != null ? interaction.getOpportunityId().value() : null,
				interaction.getCustomerId(),
				interaction.getChannel(),
				interaction.getSummary(),
				interaction.getTimestamp());
	}
}
