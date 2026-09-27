package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.QuoteItem;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateQuoteCommand(UUID customerId, UUID salespersonId, List<QuoteItem> items, LocalDate validUntil) {
}
