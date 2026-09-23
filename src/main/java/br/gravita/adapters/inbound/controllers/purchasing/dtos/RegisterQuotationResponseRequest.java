package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterQuotationResponseRequest(
		@NotNull UUID supplierId,
		@NotEmpty List<@Valid ItemPriceRequest> itemPrices,
		@NotNull LocalDate deadline) {

	public RegisterQuotationResponseCommand toCommand(UUID quotationId) {
		return new RegisterQuotationResponseCommand(
				QuotationId.of(quotationId),
				SupplierId.of(supplierId),
				itemPrices.stream().map(ItemPriceRequest::toDomain).toList(),
				deadline);
	}

	public record ItemPriceRequest(@NotNull UUID productId, @NotNull @PositiveOrZero BigDecimal unitPrice) {

		QuotationItemPrice toDomain() {
			return new QuotationItemPrice(productId, unitPrice);
		}
	}
}
