package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FollowUpTaskTest {

	@Test
	@DisplayName("Schedules a task linked to an opportunity")
	void schedulesATaskLinkedToAnOpportunity() {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());
		final UUID opportunityId = UUID.randomUUID();
		final UUID owner = UUID.randomUUID();

		final FollowUpTask task = FollowUpTask.schedule(id, opportunityId, null, LocalDate.now().plusDays(3), owner,
				AlertChannel.EMAIL);

		assertThat(task.getOpportunityId()).isEqualTo(opportunityId);
		assertThat(task.getCustomerId()).isNull();
		assertThat(task.getOwner()).isEqualTo(owner);
		assertThat(task.getAlertChannel()).isEqualTo(AlertChannel.EMAIL);
	}

	@Test
	@DisplayName("Schedules a task linked to a customer")
	void schedulesATaskLinkedToACustomer() {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());
		final UUID customerId = UUID.randomUUID();

		final FollowUpTask task = FollowUpTask.schedule(id, null, customerId, LocalDate.now(), UUID.randomUUID(),
				AlertChannel.APP);

		assertThat(task.getCustomerId()).isEqualTo(customerId);
		assertThat(task.getOpportunityId()).isNull();
	}

	@Test
	@DisplayName("Rejects a task linked to neither an opportunity nor a customer")
	void rejectsATaskWithNeitherOpportunityNorCustomer() {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpTask.schedule(id, null, null, LocalDate.now(), UUID.randomUUID(),
				AlertChannel.APP))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Opportunity or a Customer");
	}

	@Test
	@DisplayName("Rejects a task with a due date in the past")
	void rejectsAPastDueDate() {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		assertThatThrownBy(() -> FollowUpTask.schedule(id, UUID.randomUUID(), null, LocalDate.now().minusDays(1),
				UUID.randomUUID(), AlertChannel.EMAIL))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("dueDate cannot be in the past");
	}

	@Test
	@DisplayName("Reconstituting from persistence does not re-reject a due date that has since become overdue")
	void reconstitutingFromPersistenceDoesNotReRejectAPastDueDateThatIsNowOverdue() {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());

		final FollowUpTask task = FollowUpTask.of(id, UUID.randomUUID(), null, LocalDate.now().minusDays(5),
				UUID.randomUUID(), AlertChannel.EMAIL);

		assertThat(task.getDueDate()).isEqualTo(LocalDate.now().minusDays(5));
	}
}
