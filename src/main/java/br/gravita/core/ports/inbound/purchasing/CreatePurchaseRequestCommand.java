package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requestedBy} is null when {@code origin} is a system trigger
 * ({@code MIN_STOCK_TRIGGER}/{@code SALES_ORDER_DEMAND}); the domain enforces
 * that consistency, this command only carries the caller's input through.
 */
public record CreatePurchaseRequestCommand(
		PurchaseRequestOrigin origin,
		List<PurchaseRequestItem> items,
		UUID requestedBy) {

	public CreatePurchaseRequestCommand {
		Objects.requireNonNull(origin, "origin is required");
		items = items == null ? List.of() : List.copyOf(items);
	}
}
