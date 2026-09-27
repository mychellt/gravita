package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.OpportunityId;

public record GetOpportunityQuery(OpportunityId opportunityId) {
}
