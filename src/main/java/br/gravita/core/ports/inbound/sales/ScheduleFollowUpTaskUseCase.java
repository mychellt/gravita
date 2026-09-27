package br.gravita.core.ports.inbound.sales;

public interface ScheduleFollowUpTaskUseCase {
	FollowUpTaskView execute(ScheduleFollowUpTaskCommand command);
}
