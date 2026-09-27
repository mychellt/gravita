package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FollowUpRuleTest {

	@Test
	void createsARuleWithValidFields() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());

		FollowUpRule rule = FollowUpRule.of(id, 7, FollowUpTarget.CUSTOMER, true, true);

		assertThat(rule.getDaysWithoutContact()).isEqualTo(7);
		assertThat(rule.getTarget()).isEqualTo(FollowUpTarget.CUSTOMER);
		assertThat(rule.isNotifyOwner()).isTrue();
		assertThat(rule.isActive()).isTrue();
	}

	@Test
	void rejectsZeroDaysWithoutContact() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpRule.of(id, 0, FollowUpTarget.CUSTOMER, true, true))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("daysWithoutContact");
	}

	@Test
	void rejectsNegativeDaysWithoutContact() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpRule.of(id, -1, FollowUpTarget.OPPORTUNITY, false, false))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("daysWithoutContact");
	}

	@Test
	void partialUpdateKeepsUnspecifiedFieldsUnchanged() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		FollowUpRule rule = FollowUpRule.of(id, 7, FollowUpTarget.CUSTOMER, true, true);

		FollowUpRule updated = rule.withUpdatedFields(14, null, null, null);

		assertThat(updated.getDaysWithoutContact()).isEqualTo(14);
		assertThat(updated.getTarget()).isEqualTo(FollowUpTarget.CUSTOMER);
		assertThat(updated.isNotifyOwner()).isTrue();
		assertThat(updated.isActive()).isTrue();
	}

	@Test
	void updateCanDeactivateARule() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		FollowUpRule rule = FollowUpRule.of(id, 7, FollowUpTarget.CUSTOMER, true, true);

		FollowUpRule updated = rule.withUpdatedFields(null, null, null, false);

		assertThat(updated.isActive()).isFalse();
	}
}
