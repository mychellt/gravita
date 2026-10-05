package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.SettleTitleCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record SettleTitleRequest(
		@NotNull @Positive BigDecimal amount,
		@PositiveOrZero BigDecimal interest,
		@PositiveOrZero BigDecimal fine,
		@PositiveOrZero BigDecimal discount,
		@PositiveOrZero BigDecimal surcharge,
		boolean partial) {

	public SettleTitleCommand toCommand(final UUID receivableId) {
		return new SettleTitleCommand(receivableId, amount, interest, fine, discount, surcharge, partial);
	}
}
