package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;

import java.util.UUID;

public record SearchProductQuery(String searchTerm, UUID customerId) {

	public SearchProductQuery {
		if (searchTerm == null || searchTerm.isBlank()) {
			throw new BusinessRuleException("searchTerm is required");
		}
	}
}
