package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FollowUpTaskTest {

	@Test
	void schedulesATaskLinkedToAnOpportunity() {
		FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());
		UUID opportunityId = UUID.randomUUID();
		UUID owner = UUID.randomUUID();

		FollowUpTask task = FollowUpTask.schedule(id, opportunityId, null, LocalDate.now().plusDays(3), owner,
				AlertChannel.EMAIL);

		assertThat(task.getOpportunityId()).isEqualTo(opportunityId);
		assertThat(task.getCustomerId()).isNull();
		assertThat(task.getOwner()).isEqualTo(owner);
		assertThat(task.getAlertChannel()).isEqualTo(AlertChannel.EMAIL);
	}

	@Test
	void schedulesATaskLinkedToACustomer() {
		FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());
		UUID customerId = UUID.randomUUID();

		FollowUpTask task = FollowUpTask.schedule(id, null, customerId, LocalDate.now(), UUID.randomUUID(),
				AlertChannel.APP);

		assertThat(task.getCustomerId()).isEqualTo(customerId);
		assertThat(task.getOpportunityId()).isNull();
	}

	@Test
	void rejectsATaskWithNeitherOpportunityNorCustomer() {
		FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpTask.schedule(id, null, null, LocalDate.now(), UUID.randomUUID(),
				AlertChannel.APP))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Opportunity or a Customer");
	}

	@Test
	void rejectsAPastDueDate() {
		FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpTask.schedule(id, UUID.randomUUID(), null, LocalDate.now().minusDays(1),
				UUID.randomUUID(), AlertChannel.EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("dueDate cannot be in the past");
	}

	@Test
	void reconstitutingFromPersistenceDoesNotReRejectAPastDueDateThatIsNowOverdue() {
		FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		FollowUpTask task = FollowUpTask.of(id, UUID.randomUUID(), null, LocalDate.now().minusDays(5),
				UUID.randomUUID(), AlertChannel.EMAIL);

		assertThat(task.getDueDate()).isEqualTo(LocalDate.now().minusDays(5));
	}
}
