package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementResponse(UUID id, UUID cashBoxId, CashMovementDirection direction, BigDecimal amount,
		String justification, Instant timestamp) {

	public static CashMovementResponse from(CashMovement movement) {
		return new CashMovementResponse(movement.getId().value(), movement.getCashBoxId().value(),
				movement.getDirection(), movement.getAmount(), movement.getJustification(),
				movement.getTimestamp());
	}
}
