package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesQuery;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesUseCase;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import java.util.List;

@UseCase
public class ListOpportunitiesService implements ListOpportunitiesUseCase {

	private final OpportunityRepositoryPort opportunityRepositoryPort;

	public ListOpportunitiesService(OpportunityRepositoryPort opportunityRepositoryPort) {
		this.opportunityRepositoryPort = opportunityRepositoryPort;
	}

	@Override
	public List<OpportunityView> execute(ListOpportunitiesQuery query) {
		List<Opportunity> opportunities = query.stage() == null
				? opportunityRepositoryPort.findAll()
				: opportunityRepositoryPort.findByStage(query.stage());
		return opportunities.stream().map(OpportunityView::from).toList();
	}
}
