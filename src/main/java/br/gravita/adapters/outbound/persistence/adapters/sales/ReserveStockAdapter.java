package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.inbound.inventory.ReserveStockCommand;
import br.gravita.core.ports.inbound.inventory.ReserveStockUseCase;
import br.gravita.core.ports.outbound.sales.ReserveStockPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReserveStockAdapter implements ReserveStockPort {

	static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final ReserveStockUseCase reserveStockUseCase;

	ReserveStockAdapter(ReserveStockUseCase reserveStockUseCase) {
		this.reserveStockUseCase = reserveStockUseCase;
	}

	@Override
	public void reserve(ReserveStockForOrderCommand command) {
		reserveStockUseCase.execute(new ReserveStockCommand(command.orderId(), command.productOrServiceId(),
				DEFAULT_WAREHOUSE_ID, command.quantity()));
	}
}
