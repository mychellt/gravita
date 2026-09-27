package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.Commission;
import java.math.BigDecimal;
import java.util.UUID;

public record CommissionView(UUID id, UUID salespersonId, UUID productId, UUID orderId, BigDecimal rate,
		BigDecimal amount) {

	public static CommissionView from(Commission commission) {
		return new CommissionView(commission.id().value(), commission.salespersonId(), commission.productId(),
				commission.orderId().value(), commission.rate(), commission.amount());
	}
}
