package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class FollowUpRule {

	private final FollowUpRuleId id;
	private final int daysWithoutContact;
	private final FollowUpTarget target;
	private final boolean notifyOwner;
	private final boolean active;

	public FollowUpRule(FollowUpRuleId id, int daysWithoutContact, FollowUpTarget target, boolean notifyOwner,
			boolean active) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.daysWithoutContact = requirePositive(daysWithoutContact);
		this.target = Objects.requireNonNull(target, "target is required");
		this.notifyOwner = notifyOwner;
		this.active = active;
	}

	public static FollowUpRule of(FollowUpRuleId id, int daysWithoutContact, FollowUpTarget target,
			boolean notifyOwner, boolean active) {
		return new FollowUpRule(id, daysWithoutContact, target, notifyOwner, active);
	}

	public FollowUpRule withUpdatedFields(Integer daysWithoutContact, FollowUpTarget target, Boolean notifyOwner,
			Boolean active) {
		return new FollowUpRule(id,
				daysWithoutContact != null ? daysWithoutContact : this.daysWithoutContact,
				target != null ? target : this.target,
				notifyOwner != null ? notifyOwner : this.notifyOwner,
				active != null ? active : this.active);
	}

	private static int requirePositive(int daysWithoutContact) {
		if (daysWithoutContact <= 0) {
			throw new BusinessRuleException("daysWithoutContact must be a positive integer: " + daysWithoutContact);
		}
		return daysWithoutContact;
	}
}
