package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.ports.inbound.tax.ClosePosSessionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record ClosePosSessionRequest(List<@Valid CountedAmount> closingCountedAmounts) {

	public record CountedAmount(@NotNull PaymentMethodType method, @NotNull @PositiveOrZero BigDecimal amount) {
	}

	public ClosePosSessionRequest {
		closingCountedAmounts = closingCountedAmounts == null ? List.of() : List.copyOf(closingCountedAmounts);
	}

	public ClosePosSessionCommand toCommand(UUID sessionId) {
		Map<PaymentMethodType, BigDecimal> countedAmounts = closingCountedAmounts.stream()
				.collect(Collectors.toMap(CountedAmount::method, CountedAmount::amount));
		return new ClosePosSessionCommand(sessionId, countedAmounts);
	}
}
