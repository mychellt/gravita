package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.PaymentMethodType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ClosePosSessionCommand(UUID sessionId, Map<PaymentMethodType, BigDecimal> closingCountedAmounts) {

	public ClosePosSessionCommand {
		Objects.requireNonNull(sessionId, "sessionId is required");
		closingCountedAmounts = closingCountedAmounts == null ? Map.of() : Map.copyOf(closingCountedAmounts);
	}
}
