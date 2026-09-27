package br.gravita.core.ports.outbound.persistence.sales;

import java.util.Objects;
import java.util.UUID;

public interface ReleaseStockReservationPort {
	void release(ReleaseStockReservationCommand command);

	record ReleaseStockReservationCommand(UUID reservationId) {

		public ReleaseStockReservationCommand {
			Objects.requireNonNull(reservationId, "reservationId is required");
		}
	}
}
