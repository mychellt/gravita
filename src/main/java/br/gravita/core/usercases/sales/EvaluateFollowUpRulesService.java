package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.ports.inbound.sales.EvaluateFollowUpRulesUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpTaskRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.InteractionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule targets are class-wide (every {@code CUSTOMER} or every {@code OPPORTUNITY}), not tied to a
 * specific record, so evaluating them means enumerating the target population via
 * {@link OpportunityRepositoryPort}/{@link CustomerRepositoryPort} - reused here even though UC-M7-12's
 * spec only calls out {@link FollowUpRuleRepositoryPort}, {@link InteractionRepositoryPort} and
 * {@link SendFollowUpAlertPort}, since there is no other way to discover a target with zero interactions.
 * A customer has no owning-salesperson field of its own, so for {@code CUSTOMER} rules the owner is
 * resolved through any {@link Opportunity} linked to that customer; a customer with none is skipped.
 * Duplicate alerts across runs are avoided by persisting the breach as a {@link FollowUpTask} due today
 * via {@link FollowUpTaskRepositoryPort} and checking for one already recorded before sending again.
 */
@UseCase
public class EvaluateFollowUpRulesService implements EvaluateFollowUpRulesUseCase {

	private final FollowUpRuleRepositoryPort followUpRuleRepositoryPort;
	private final InteractionRepositoryPort interactionRepositoryPort;
	private final OpportunityRepositoryPort opportunityRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final FollowUpTaskRepositoryPort followUpTaskRepositoryPort;
	private final SendFollowUpAlertPort sendFollowUpAlertPort;

	public EvaluateFollowUpRulesService(FollowUpRuleRepositoryPort followUpRuleRepositoryPort,
			InteractionRepositoryPort interactionRepositoryPort, OpportunityRepositoryPort opportunityRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort, FollowUpTaskRepositoryPort followUpTaskRepositoryPort,
			SendFollowUpAlertPort sendFollowUpAlertPort) {
		this.followUpRuleRepositoryPort = followUpRuleRepositoryPort;
		this.interactionRepositoryPort = interactionRepositoryPort;
		this.opportunityRepositoryPort = opportunityRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.followUpTaskRepositoryPort = followUpTaskRepositoryPort;
		this.sendFollowUpAlertPort = sendFollowUpAlertPort;
	}

	@Override
	public void execute() {
		for (FollowUpRule rule : followUpRuleRepositoryPort.findAllActive()) {
			if (!rule.isNotifyOwner()) {
				continue;
			}
			if (rule.getTarget() == FollowUpTarget.OPPORTUNITY) {
				evaluateOpportunities(rule);
			} else {
				evaluateCustomers(rule);
			}
		}
	}

	private void evaluateOpportunities(FollowUpRule rule) {
		for (Opportunity opportunity : opportunityRepositoryPort.findAll()) {
			List<Interaction> interactions = interactionRepositoryPort.findByOpportunityId(opportunity.getId());
			if (isBreached(interactions, rule.getDaysWithoutContact())) {
				notifyBreach(opportunity.getId().value(), null, opportunity.getOwner());
			}
		}
	}

	private void evaluateCustomers(FollowUpRule rule) {
		List<Opportunity> opportunities = opportunityRepositoryPort.findAll();
		for (CustomerDomain customer : customerRepositoryPort.findAll()) {
			UUID customerId = customer.getId();
			List<Interaction> interactions = interactionRepositoryPort.findByCustomerId(customerId);
			if (isBreached(interactions, rule.getDaysWithoutContact())) {
				findOwner(opportunities, customerId).ifPresent(owner -> notifyBreach(null, customerId, owner));
			}
		}
	}

	private Optional<UUID> findOwner(List<Opportunity> opportunities, UUID customerId) {
		return opportunities.stream()
				.filter(opportunity -> opportunity.getCustomerId().equals(customerId))
				.map(Opportunity::getOwner)
				.findFirst();
	}

	private boolean isBreached(List<Interaction> interactions, int daysWithoutContact) {
		if (interactions.isEmpty()) {
			return true;
		}
		Instant lastContact = interactions.stream().map(Interaction::getTimestamp).max(Instant::compareTo)
				.orElseThrow();
		return lastContact.isBefore(Instant.now().minus(daysWithoutContact, ChronoUnit.DAYS));
	}

	private void notifyBreach(UUID opportunityId, UUID customerId, UUID owner) {
		LocalDate today = LocalDate.now();
		if (followUpTaskRepositoryPort.existsForTargetOnDate(opportunityId, customerId, today)) {
			return;
		}
		FollowUpTask task = FollowUpTask.of(FollowUpTaskId.of(UUID.randomUUID()), opportunityId, customerId, today,
				owner, AlertChannel.APP);
		FollowUpTask saved = followUpTaskRepositoryPort.save(task);
		sendFollowUpAlertPort.send(saved);
	}
}
