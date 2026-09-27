package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.ports.inbound.sales.ScheduleFollowUpTaskCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduleFollowUpTaskRequest(
		UUID opportunityId,
		UUID customerId,
		@NotNull LocalDate dueDate,
		@NotNull UUID owner,
		@NotNull AlertChannel alertChannel) {

	public ScheduleFollowUpTaskCommand toCommand() {
		return ScheduleFollowUpTaskCommand.builder()
				.opportunityId(opportunityId)
				.customerId(customerId)
				.dueDate(dueDate)
				.owner(owner)
				.alertChannel(alertChannel)
				.build();
	}
}
