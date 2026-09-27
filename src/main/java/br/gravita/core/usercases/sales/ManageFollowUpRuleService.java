package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpRuleNotFoundException;
import br.gravita.core.ports.inbound.sales.CreateFollowUpRuleCommand;
import br.gravita.core.ports.inbound.sales.FollowUpRuleView;
import br.gravita.core.ports.inbound.sales.ManageFollowUpRuleUseCase;
import br.gravita.core.ports.inbound.sales.UpdateFollowUpRuleCommand;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import java.util.UUID;

@UseCase
public class ManageFollowUpRuleService implements ManageFollowUpRuleUseCase {

	private final FollowUpRuleRepositoryPort followUpRuleRepositoryPort;

	public ManageFollowUpRuleService(FollowUpRuleRepositoryPort followUpRuleRepositoryPort) {
		this.followUpRuleRepositoryPort = followUpRuleRepositoryPort;
	}

	@Override
	public FollowUpRuleView create(CreateFollowUpRuleCommand command) {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		FollowUpRule rule = FollowUpRule.of(id, command.daysWithoutContact(), command.target(),
				command.notifyOwner(), command.active());

		FollowUpRule saved = followUpRuleRepositoryPort.save(rule);
		return FollowUpRuleView.from(saved);
	}

	@Override
	public FollowUpRuleView update(UpdateFollowUpRuleCommand command) {
		FollowUpRule existing = findOrThrow(command.ruleId());

		FollowUpRule updated = existing.withUpdatedFields(command.daysWithoutContact(), command.target(),
				command.notifyOwner(), command.active());

		FollowUpRule saved = followUpRuleRepositoryPort.save(updated);
		return FollowUpRuleView.from(saved);
	}

	@Override
	public void delete(FollowUpRuleId ruleId) {
		findOrThrow(ruleId);
		followUpRuleRepositoryPort.deleteById(ruleId);
	}

	private FollowUpRule findOrThrow(FollowUpRuleId ruleId) {
		return followUpRuleRepositoryPort.findById(ruleId)
				.orElseThrow(() -> new FollowUpRuleNotFoundException(ruleId.value()));
	}
}
