package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.QuotationId;

public interface SendQuotationUseCase {
	QuotationId execute(SendQuotationCommand command);
}
