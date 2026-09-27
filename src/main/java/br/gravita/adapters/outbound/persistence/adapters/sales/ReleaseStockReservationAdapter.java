package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import br.gravita.core.ports.outbound.persistence.sales.ReleaseStockReservationPort;
import org.springframework.stereotype.Component;

@Component
class ReleaseStockReservationAdapter implements ReleaseStockReservationPort {

	private final ReleaseStockReservationUseCase releaseStockReservationUseCase;

	ReleaseStockReservationAdapter(ReleaseStockReservationUseCase releaseStockReservationUseCase) {
		this.releaseStockReservationUseCase = releaseStockReservationUseCase;
	}

	@Override
	public void release(ReleaseStockReservationCommand command) {
		releaseStockReservationUseCase.execute(
				new br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand(command.reservationId()));
	}
}
