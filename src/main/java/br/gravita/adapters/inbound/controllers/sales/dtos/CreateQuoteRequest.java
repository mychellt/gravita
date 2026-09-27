package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.ports.inbound.sales.CreateQuoteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateQuoteRequest(
		@NotNull UUID customerId,
		@NotNull UUID salespersonId,
		@NotEmpty List<@Valid ItemRequest> items,
		@NotNull LocalDate validUntil) {

	public CreateQuoteCommand toCommand() {
		return new CreateQuoteCommand(customerId, salespersonId, items.stream().map(ItemRequest::toDomain).toList(),
				validUntil);
	}

	public record ItemRequest(
			@NotNull UUID productOrServiceId,
			@NotNull @Positive @Digits(integer = 10, fraction = 4) BigDecimal quantity,
			@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 4) BigDecimal unitPrice,
			@PositiveOrZero @Digits(integer = 10, fraction = 4) BigDecimal discount) {

		QuoteItem toDomain() {
			return new QuoteItem(productOrServiceId, quantity, unitPrice, discount);
		}
	}
}
