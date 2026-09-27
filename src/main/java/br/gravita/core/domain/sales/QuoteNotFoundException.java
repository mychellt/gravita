package br.gravita.core.domain.sales;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {

	public QuoteNotFoundException(UUID quoteId) {
		super("Quote not found: " + quoteId);
	}
}
