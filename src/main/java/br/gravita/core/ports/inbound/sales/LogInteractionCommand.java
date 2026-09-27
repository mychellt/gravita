package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.InteractionChannel;
import br.gravita.core.domain.sales.OpportunityId;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LogInteractionCommand(OpportunityId opportunityId, UUID customerId, InteractionChannel channel,
		String summary, Instant timestamp) {
}
