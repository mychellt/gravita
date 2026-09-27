package br.gravita.core.ports.inbound.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateOpportunityCommand(UUID customerId, BigDecimal estimatedValue, Integer probability,
		LocalDate expectedCloseDate, UUID owner) {
}
