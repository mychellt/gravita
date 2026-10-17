package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;

import java.util.UUID;

/** A blank term matches every active product (the PDV catalog listing). */
public record SearchProductQuery(String searchTerm, UUID customerId) {

	public SearchProductQuery {
		if (searchTerm == null) {
			throw new BusinessRuleException("searchTerm is required");
		}
	}
}
