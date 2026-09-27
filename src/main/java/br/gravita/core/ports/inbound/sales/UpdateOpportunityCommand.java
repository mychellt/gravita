package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.OpportunityId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateOpportunityCommand(OpportunityId opportunityId, UUID customerId, BigDecimal estimatedValue,
		Integer probability, LocalDate expectedCloseDate, UUID owner) {
}
