package br.gravita.core.ports.inbound.sales;

import java.util.List;

public interface ListOpportunitiesUseCase {
	List<OpportunityListItem> execute(ListOpportunitiesQuery query);
}
