package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.system.UserId;
import java.util.Objects;
import java.util.UUID;

public record GetProductPurchaseHistoryQuery(UUID productId, UserId callerId) {

	public GetProductPurchaseHistoryQuery {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(callerId, "callerId is required");
	}
}
