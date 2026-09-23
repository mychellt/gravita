package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import java.util.Optional;

public interface QuotationRepositoryPort {
	Quotation save(Quotation quotation);
	Optional<Quotation> findById(QuotationId id);
}
