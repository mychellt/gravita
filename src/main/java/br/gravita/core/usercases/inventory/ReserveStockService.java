package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.ports.inbound.inventory.ReserveStockCommand;
import br.gravita.core.ports.inbound.inventory.ReserveStockUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;

import java.util.UUID;

@UseCase
public class ReserveStockService implements ReserveStockUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockReservationRepositoryPort stockReservationRepositoryPort;
	private final LowStockReorderTrigger lowStockReorderTrigger;

	public ReserveStockService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			StockReservationRepositoryPort stockReservationRepositoryPort,
			LowStockReorderTrigger lowStockReorderTrigger) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockReservationRepositoryPort = stockReservationRepositoryPort;
		this.lowStockReorderTrigger = lowStockReorderTrigger;
	}

	@Override
	public StockReservation execute(ReserveStockCommand command) {
		StockBalance balance = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
				.orElseThrow(() -> new BusinessRuleException(
						"Insufficient available stock to reserve: requested " + command.quantity()
								+ ", available 0"));

		StockBalance reserved = balance.reserve(command.quantity());
		stockBalanceRepositoryPort.save(reserved);

		StockReservation reservation = StockReservation.create(StockReservationId.of(UUID.randomUUID()),
				command.orderRef(), command.productId(), command.warehouseId(), command.quantity());
		StockReservation saved = stockReservationRepositoryPort.save(reservation);
		lowStockReorderTrigger.evaluate(command.warehouseId());
		return saved;
	}
}
