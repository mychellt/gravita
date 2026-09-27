package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.AlertChannel;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ScheduleFollowUpTaskCommand(UUID opportunityId, UUID customerId, LocalDate dueDate, UUID owner,
		AlertChannel alertChannel) {
}
