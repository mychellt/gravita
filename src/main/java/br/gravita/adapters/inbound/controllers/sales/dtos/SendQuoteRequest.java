package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.QuoteDeliveryChannel;
import br.gravita.core.ports.inbound.sales.SendQuoteCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendQuoteRequest(@NotNull QuoteDeliveryChannel channel) {

	public SendQuoteCommand toCommand(UUID quoteId) {
		return new SendQuoteCommand(quoteId, channel);
	}
}
