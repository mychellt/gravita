package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.inbound.sales.OpportunityListItem;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityListItemResponse(UUID id, UUID customerId, String customerName, BigDecimal estimatedValue,
		Integer probability, LocalDate expectedCloseDate, UUID owner, String ownerName, OpportunityStage stage) {

	public static OpportunityListItemResponse from(final OpportunityListItem item) {
		final OpportunityView view = item.opportunity();
		return new OpportunityListItemResponse(view.id(), view.customerId(), item.customerName(), view.estimatedValue(),
				view.probability(), view.expectedCloseDate(), view.owner(), item.ownerName(), view.stage());
	}
}
