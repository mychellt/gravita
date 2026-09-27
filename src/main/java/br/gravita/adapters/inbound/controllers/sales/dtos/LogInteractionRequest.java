package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.InteractionChannel;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.ports.inbound.sales.LogInteractionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record LogInteractionRequest(
		UUID customerId,
		@NotNull InteractionChannel channel,
		@NotBlank String summary,
		@NotNull Instant timestamp) {

	public LogInteractionCommand toCommand(UUID opportunityId) {
		return LogInteractionCommand.builder()
				.opportunityId(opportunityId != null ? OpportunityId.of(opportunityId) : null)
				.customerId(customerId)
				.channel(channel)
				.summary(summary)
				.timestamp(timestamp)
				.build();
	}
}
