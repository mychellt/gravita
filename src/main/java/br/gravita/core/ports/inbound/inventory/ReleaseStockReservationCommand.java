package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

public record ReleaseStockReservationCommand(UUID reservationId) {

	public ReleaseStockReservationCommand {
		Objects.requireNonNull(reservationId, "reservationId is required");
	}
}
