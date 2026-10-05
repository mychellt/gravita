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
import java.util.List;

@UseCase
public class ReleaseStockReservationService implements ReleaseStockReservationUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockReservationRepositoryPort stockReservationRepositoryPort;

	public ReleaseStockReservationService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final StockReservationRepositoryPort stockReservationRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockReservationRepositoryPort = stockReservationRepositoryPort;
	}

	@Override
	public void execute(final ReleaseStockReservationCommand command) {
		for (final StockReservation reservation : resolveReservations(command)) {
			release(reservation);
		}
	}

	private List<StockReservation> resolveReservations(final ReleaseStockReservationCommand command) {
		if (command.reservationId() != null) {
			final StockReservationId id = StockReservationId.of(command.reservationId());
			return List.of(stockReservationRepositoryPort.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Stock reservation not found: " + id.value())));
		}
		return stockReservationRepositoryPort.findActiveByOrderRef(command.orderRef());
	}

	private void release(final StockReservation reservation) {
		final StockReservation released = reservation.release();

		final StockBalance balance = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(reservation.getProductId(), reservation.getWarehouseId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Stock balance not found for product " + reservation.getProductId() + " and warehouse "
								+ reservation.getWarehouseId()));

		stockBalanceRepositoryPort.save(balance.release(reservation.getQuantity()));
		stockReservationRepositoryPort.save(released);
	}
}
