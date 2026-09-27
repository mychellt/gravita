package br.gravita.core.ports.inbound.sales;

public interface CreateQuoteUseCase {
	QuoteView execute(CreateQuoteCommand command);
}
