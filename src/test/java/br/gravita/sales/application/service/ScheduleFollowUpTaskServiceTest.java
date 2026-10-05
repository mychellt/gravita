package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.FollowUpTaskView;
import br.gravita.core.ports.inbound.sales.ScheduleFollowUpTaskCommand;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpTaskRepositoryPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import br.gravita.core.usercases.sales.ScheduleFollowUpTaskService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleFollowUpTaskServiceTest {

	@Mock
	private FollowUpTaskRepositoryPort followUpTaskRepositoryPort;

	@Mock
	private SendFollowUpAlertPort sendFollowUpAlertPort;

	@InjectMocks
	private ScheduleFollowUpTaskService service;

	@Test
	@DisplayName("Schedules a follow-up task and dispatches the alert")
	void schedulesATaskAndDispatchesTheAlert() {
		when(followUpTaskRepositoryPort.save(any(FollowUpTask.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		final UUID opportunityId = UUID.randomUUID();
		final UUID owner = UUID.randomUUID();
		final ScheduleFollowUpTaskCommand command = ScheduleFollowUpTaskCommand.builder()
				.opportunityId(opportunityId)
				.dueDate(LocalDate.now().plusDays(5))
				.owner(owner)
				.alertChannel(AlertChannel.EMAIL)
				.build();

		final FollowUpTaskView view = service.execute(command);

		assertThat(view.opportunityId()).isEqualTo(opportunityId);
		assertThat(view.owner()).isEqualTo(owner);
		assertThat(view.alertChannel()).isEqualTo(AlertChannel.EMAIL);

		final ArgumentCaptor<FollowUpTask> sentTask = ArgumentCaptor.forClass(FollowUpTask.class);
		verify(sendFollowUpAlertPort).send(sentTask.capture());
		assertThat(sentTask.getValue().getOpportunityId()).isEqualTo(opportunityId);
	}

	@Test
	@DisplayName("Rejects scheduling a task with a due date in the past")
	void rejectsSchedulingWithAPastDueDate() {
		final ScheduleFollowUpTaskCommand command = ScheduleFollowUpTaskCommand.builder()
				.customerId(UUID.randomUUID())
				.dueDate(LocalDate.now().minusDays(1))
				.owner(UUID.randomUUID())
				.alertChannel(AlertChannel.APP)
				.build();

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(followUpTaskRepositoryPort, never()).save(any());
		verify(sendFollowUpAlertPort, never()).send(any());
	}

	@Test
	@DisplayName("Rejects scheduling a task linked to neither an opportunity nor a customer")
	void rejectsSchedulingWithoutAnOpportunityOrCustomerLink() {
		final ScheduleFollowUpTaskCommand command = ScheduleFollowUpTaskCommand.builder()
				.dueDate(LocalDate.now().plusDays(1))
				.owner(UUID.randomUUID())
				.alertChannel(AlertChannel.APP)
				.build();

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(followUpTaskRepositoryPort, never()).save(any());
	}
}
