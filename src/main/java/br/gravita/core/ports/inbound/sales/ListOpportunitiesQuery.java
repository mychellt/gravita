package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.OpportunityStage;

public record ListOpportunitiesQuery(OpportunityStage stage) {

	public static ListOpportunitiesQuery all() {
		return new ListOpportunitiesQuery(null);
	}
}
