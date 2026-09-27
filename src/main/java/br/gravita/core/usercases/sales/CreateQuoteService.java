package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.ports.inbound.sales.CreateQuoteCommand;
import br.gravita.core.ports.inbound.sales.CreateQuoteUseCase;
import br.gravita.core.ports.inbound.sales.QuoteView;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@UseCase
public class CreateQuoteService implements CreateQuoteUseCase {

	private final QuoteRepositoryPort quoteRepositoryPort;

	@Override
	public QuoteView execute(CreateQuoteCommand command) {
		var quote = Quote.create(QuoteId.of(UUID.randomUUID()), command.customerId(), command.salespersonId(),
				command.items(), command.validUntil(), LocalDate.now());
		return QuoteView.from(quoteRepositoryPort.save(quote));
	}
}
