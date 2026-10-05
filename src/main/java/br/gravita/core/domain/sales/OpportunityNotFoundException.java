package br.gravita.core.domain.sales;

import java.util.UUID;

public class OpportunityNotFoundException extends RuntimeException {

	public OpportunityNotFoundException(final UUID opportunityId) {
		super("Opportunity not found: " + opportunityId);
	}
}
