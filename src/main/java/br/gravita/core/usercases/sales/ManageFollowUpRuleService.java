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

	public ManageFollowUpRuleService(final FollowUpRuleRepositoryPort followUpRuleRepositoryPort) {
		this.followUpRuleRepositoryPort = followUpRuleRepositoryPort;
	}

	@Override
	public FollowUpRuleView create(final CreateFollowUpRuleCommand command) {
		final FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		final FollowUpRule rule = FollowUpRule.of(id, command.daysWithoutContact(), command.target(),
				command.notifyOwner(), command.active());

		final FollowUpRule saved = followUpRuleRepositoryPort.save(rule);
		return FollowUpRuleView.from(saved);
	}

	@Override
	public FollowUpRuleView update(final UpdateFollowUpRuleCommand command) {
		final FollowUpRule existing = findOrThrow(command.ruleId());

		final FollowUpRule updated = existing.withUpdatedFields(command.daysWithoutContact(), command.target(),
				command.notifyOwner(), command.active());

		final FollowUpRule saved = followUpRuleRepositoryPort.save(updated);
		return FollowUpRuleView.from(saved);
	}

	@Override
	public void delete(final FollowUpRuleId ruleId) {
		findOrThrow(ruleId);
		followUpRuleRepositoryPort.deleteById(ruleId);
	}

	private FollowUpRule findOrThrow(final FollowUpRuleId ruleId) {
		return followUpRuleRepositoryPort.findById(ruleId)
				.orElseThrow(() -> new FollowUpRuleNotFoundException(ruleId.value()));
	}
}
