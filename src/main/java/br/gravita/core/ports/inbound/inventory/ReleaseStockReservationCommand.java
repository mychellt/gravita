package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

public record ReleaseStockReservationCommand(UUID reservationId, UUID orderRef) {

	public ReleaseStockReservationCommand {
		if (reservationId == null && orderRef == null) {
			throw new NullPointerException("either reservationId or orderRef is required");
		}
	}

	public ReleaseStockReservationCommand(final UUID reservationId) {
		this(reservationId, null);
	}

	public static ReleaseStockReservationCommand byOrderRef(final UUID orderRef) {
		Objects.requireNonNull(orderRef, "orderRef is required");
		return new ReleaseStockReservationCommand(null, orderRef);
	}
}
