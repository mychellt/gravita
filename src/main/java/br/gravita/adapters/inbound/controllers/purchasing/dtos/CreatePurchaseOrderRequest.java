package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
		@NotNull UUID requestId,
		UUID quotationId,
		@NotNull UUID supplierId,
		@NotEmpty List<@Valid ItemRequest> items) {

	public CreatePurchaseOrderCommand toCommand() {
		return new CreatePurchaseOrderCommand(
				PurchaseRequestId.of(requestId),
				quotationId,
				SupplierId.of(supplierId),
				items.stream().map(ItemRequest::toDomain).toList());
	}

	public record ItemRequest(
			@NotNull UUID productId,
			@NotNull @Positive BigDecimal quantity,
			@NotNull @PositiveOrZero BigDecimal unitPrice) {

		PurchaseOrderItem toDomain() {
			return new PurchaseOrderItem(productId, quantity, unitPrice);
		}
	}
}
