package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.QuoteDeliveryChannel;
import java.util.Objects;
import java.util.UUID;

public record SendQuoteCommand(UUID quoteId, QuoteDeliveryChannel channel) {

	public SendQuoteCommand {
		Objects.requireNonNull(quoteId, "quoteId is required");
		Objects.requireNonNull(channel, "channel is required");
	}
}
