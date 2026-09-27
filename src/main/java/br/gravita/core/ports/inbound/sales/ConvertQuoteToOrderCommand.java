package br.gravita.core.ports.inbound.sales;

import java.util.UUID;

public record ConvertQuoteToOrderCommand(UUID quoteId) {
}
