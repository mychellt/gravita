package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import java.util.Optional;

public interface QuoteRepositoryPort {
	Quote save(Quote quote);
	Optional<Quote> findById(QuoteId id);
}
