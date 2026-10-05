package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import br.gravita.core.ports.outbound.sales.ReleaseStockReservationPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReleaseStockReservationAdapter implements ReleaseStockReservationPort {

	private final ReleaseStockReservationUseCase releaseStockReservationUseCase;

	ReleaseStockReservationAdapter(final ReleaseStockReservationUseCase releaseStockReservationUseCase) {
		this.releaseStockReservationUseCase = releaseStockReservationUseCase;
	}

	@Override
	public void releaseByOrderRef(final UUID orderId) {
		releaseStockReservationUseCase.execute(ReleaseStockReservationCommand.byOrderRef(orderId));
	}
}
