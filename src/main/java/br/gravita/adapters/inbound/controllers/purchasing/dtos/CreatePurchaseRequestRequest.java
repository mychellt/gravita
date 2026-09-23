package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequestRequest(
		@NotNull PurchaseRequestOrigin origin,
		@NotEmpty List<@Valid ItemRequest> items,
		UUID requestedBy) {

	public CreatePurchaseRequestCommand toCommand() {
		return new CreatePurchaseRequestCommand(
				origin,
				items.stream().map(ItemRequest::toDomain).toList(),
				requestedBy);
	}

	public record ItemRequest(@NotNull UUID productId, @NotNull @Positive BigDecimal quantity) {

		PurchaseRequestItem toDomain() {
			return new PurchaseRequestItem(productId, quantity);
		}
	}
}
