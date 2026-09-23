package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand.ReturnedItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReturnToSupplierRequest(@NotEmpty List<@Valid ItemRequest> items) {

	public ReturnToSupplierCommand toCommand(UUID receiptId) {
		return new ReturnToSupplierCommand(PurchaseReceiptId.of(receiptId),
				items.stream().map(ItemRequest::toDomain).toList());
	}

	public record ItemRequest(@NotNull UUID productId, @NotNull @Positive BigDecimal quantity) {
		ReturnedItem toDomain() {
			return new ReturnedItem(productId, quantity);
		}
	}
}
