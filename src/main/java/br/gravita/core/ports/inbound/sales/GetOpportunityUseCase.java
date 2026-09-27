package br.gravita.core.ports.inbound.sales;

public interface GetOpportunityUseCase {
	OpportunityView execute(GetOpportunityQuery query);
}
