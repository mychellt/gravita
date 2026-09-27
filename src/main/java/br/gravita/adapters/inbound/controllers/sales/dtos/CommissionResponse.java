package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.CommissionView;
import java.math.BigDecimal;
import java.util.UUID;

public record CommissionResponse(UUID id, UUID salespersonId, UUID productId, UUID orderId, BigDecimal rate,
		BigDecimal amount) {

	public static CommissionResponse from(CommissionView view) {
		return new CommissionResponse(view.id(), view.salespersonId(), view.productId(), view.orderId(),
				view.rate(), view.amount());
	}
}
