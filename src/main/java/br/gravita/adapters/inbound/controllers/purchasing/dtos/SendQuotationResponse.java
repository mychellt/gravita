package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.QuotationId;
import java.util.UUID;

public record SendQuotationResponse(UUID id) {
	public static SendQuotationResponse from(final QuotationId id) {
		return new SendQuotationResponse(id.value());
	}
}
