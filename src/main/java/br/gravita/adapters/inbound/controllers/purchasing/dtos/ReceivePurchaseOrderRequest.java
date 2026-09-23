package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand.ReceivedItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReceivePurchaseOrderRequest(@NotEmpty List<@Valid ItemRequest> receivedItems) {

	public ReceivePurchaseOrderCommand toCommand(UUID orderId) {
		return new ReceivePurchaseOrderCommand(PurchaseOrderId.of(orderId),
				receivedItems.stream().map(ItemRequest::toDomain).toList());
	}

	public record ItemRequest(@NotNull UUID productId, @NotNull @Positive BigDecimal receivedQty) {
		ReceivedItem toDomain() {
			return new ReceivedItem(productId, receivedQty);
		}
	}
}
