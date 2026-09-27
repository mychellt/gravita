package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.inbound.sales.FollowUpRuleView;
import br.gravita.core.ports.inbound.sales.ListFollowUpRulesUseCase;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import java.util.List;

@UseCase
public class ListFollowUpRulesService implements ListFollowUpRulesUseCase {

	private final FollowUpRuleRepositoryPort followUpRuleRepositoryPort;

	public ListFollowUpRulesService(FollowUpRuleRepositoryPort followUpRuleRepositoryPort) {
		this.followUpRuleRepositoryPort = followUpRuleRepositoryPort;
	}

	@Override
	public List<FollowUpRuleView> execute() {
		return followUpRuleRepositoryPort.findAll().stream().map(FollowUpRuleView::from).toList();
	}
}
