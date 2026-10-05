package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class FollowUpTask {

	private final FollowUpTaskId id;
	private final UUID opportunityId;
	private final UUID customerId;
	private final LocalDate dueDate;
	private final UUID owner;
	private final AlertChannel alertChannel;

	public FollowUpTask(final FollowUpTaskId id, final UUID opportunityId, final UUID customerId, final LocalDate dueDate, final UUID owner,
			final AlertChannel alertChannel) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.opportunityId = opportunityId;
		this.customerId = requireLinkedToOpportunityOrCustomer(opportunityId, customerId);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.owner = Objects.requireNonNull(owner, "owner is required");
		this.alertChannel = Objects.requireNonNull(alertChannel, "alertChannel is required");
	}

	public static FollowUpTask schedule(final FollowUpTaskId id, final UUID opportunityId, final UUID customerId, final LocalDate dueDate,
			final UUID owner, final AlertChannel alertChannel) {
		requireNotInThePast(dueDate);
		return new FollowUpTask(id, opportunityId, customerId, dueDate, owner, alertChannel);
	}

	public static FollowUpTask of(final FollowUpTaskId id, final UUID opportunityId, final UUID customerId, final LocalDate dueDate,
			final UUID owner, final AlertChannel alertChannel) {
		return new FollowUpTask(id, opportunityId, customerId, dueDate, owner, alertChannel);
	}

	private static UUID requireLinkedToOpportunityOrCustomer(final UUID opportunityId, final UUID customerId) {
		if (opportunityId == null && customerId == null) {
			throw new BusinessRuleException("A follow-up task must be linked to an Opportunity or a Customer");
		}
		return customerId;
	}

	private static void requireNotInThePast(final LocalDate dueDate) {
		Objects.requireNonNull(dueDate, "dueDate is required");
		if (dueDate.isBefore(LocalDate.now())) {
			throw new BusinessRuleException("dueDate cannot be in the past: " + dueDate);
		}
	}
}
