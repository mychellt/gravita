package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code amount} is the principal credited against the title. {@code interest},
 * {@code fine} and {@code surcharge} are paid on top of it and {@code discount}
 * is a reduction granted on it; each may be {@code null} (none). {@code partial}
 * says whether the baixa is meant to leave part of the title unpaid.
 */
public record SettleTitleCommand(UUID receivableId, BigDecimal amount, BigDecimal interest, BigDecimal fine,
		BigDecimal discount, BigDecimal surcharge, boolean partial) {

	public SettleTitleCommand {
		Objects.requireNonNull(receivableId, "receivableId is required");
		Objects.requireNonNull(amount, "amount is required");
	}
}
