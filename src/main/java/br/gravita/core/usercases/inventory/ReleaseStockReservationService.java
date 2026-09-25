package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;

@UseCase
public class ReleaseStockReservationService implements ReleaseStockReservationUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockReservationRepositoryPort stockReservationRepositoryPort;

	public ReleaseStockReservationService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			StockReservationRepositoryPort stockReservationRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockReservationRepositoryPort = stockReservationRepositoryPort;
	}

	@Override
	public void execute(ReleaseStockReservationCommand command) {
		StockReservationId id = StockReservationId.of(command.reservationId());
		StockReservation reservation = stockReservationRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Stock reservation not found: " + id.value()));

		StockReservation released = reservation.release();

		StockBalance balance = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(reservation.getProductId(), reservation.getWarehouseId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Stock balance not found for product " + reservation.getProductId() + " and warehouse "
								+ reservation.getWarehouseId()));

		stockBalanceRepositoryPort.save(balance.release(reservation.getQuantity()));
		stockReservationRepositoryPort.save(released);
	}
}
