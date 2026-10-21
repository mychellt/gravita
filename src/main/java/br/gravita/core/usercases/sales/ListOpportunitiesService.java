package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesQuery;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesUseCase;
import br.gravita.core.ports.inbound.sales.OpportunityListItem;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class ListOpportunitiesService implements ListOpportunitiesUseCase {

	private final OpportunityRepositoryPort opportunityRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final UserRepositoryPort userRepositoryPort;

	public ListOpportunitiesService(final OpportunityRepositoryPort opportunityRepositoryPort,
			final CustomerRepositoryPort customerRepositoryPort, final UserRepositoryPort userRepositoryPort) {
		this.opportunityRepositoryPort = opportunityRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.userRepositoryPort = userRepositoryPort;
	}

	@Override
	public List<OpportunityListItem> execute(final ListOpportunitiesQuery query) {
		final List<Opportunity> opportunities = query.stage() == null
				? opportunityRepositoryPort.findAll()
				: opportunityRepositoryPort.findByStage(query.stage());
		final Map<UUID, Optional<String>> customerNames = new HashMap<>();
		final Map<UUID, Optional<String>> ownerNames = new HashMap<>();
		return opportunities.stream().map(opportunity -> new OpportunityListItem(OpportunityView.from(opportunity),
				customerNames.computeIfAbsent(opportunity.getCustomerId(),
						id -> customerRepositoryPort.get(id).map(CustomerDomain::getName)).orElse(null),
				ownerNames.computeIfAbsent(opportunity.getOwner(),
						id -> userRepositoryPort.findById(UserId.of(id)).map(User::getName)).orElse(null)))
				.toList();
	}
}
