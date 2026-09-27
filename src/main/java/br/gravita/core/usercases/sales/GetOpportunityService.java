package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityNotFoundException;
import br.gravita.core.ports.inbound.sales.GetOpportunityQuery;
import br.gravita.core.ports.inbound.sales.GetOpportunityUseCase;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;

@UseCase
public class GetOpportunityService implements GetOpportunityUseCase {

	private final OpportunityRepositoryPort opportunityRepositoryPort;

	public GetOpportunityService(OpportunityRepositoryPort opportunityRepositoryPort) {
		this.opportunityRepositoryPort = opportunityRepositoryPort;
	}

	@Override
	public OpportunityView execute(GetOpportunityQuery query) {
		Opportunity opportunity = opportunityRepositoryPort.findById(query.opportunityId())
				.orElseThrow(() -> new OpportunityNotFoundException(query.opportunityId().value()));
		return OpportunityView.from(opportunity);
	}
}
