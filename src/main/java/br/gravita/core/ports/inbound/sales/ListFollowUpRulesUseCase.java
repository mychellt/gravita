package br.gravita.core.ports.inbound.sales;

import java.util.List;

public interface ListFollowUpRulesUseCase {
	List<FollowUpRuleView> execute();
}
