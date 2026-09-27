package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.ports.inbound.sales.CancelSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.CancelSalesOrderUseCase;
import br.gravita.core.ports.outbound.persistence.sales.ReleaseStockReservationPort;
import br.gravita.core.ports.outbound.persistence.sales.ReleaseStockReservationPort.ReleaseStockReservationCommand;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CancelSalesOrderService implements CancelSalesOrderUseCase {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final ReleaseStockReservationPort releaseStockReservationPort;

	@Override
	public void execute(CancelSalesOrderCommand command) {
		SalesOrder order = salesOrderRepositoryPort.findById(SalesOrderId.of(command.orderId()))
				.orElseThrow(() -> new SalesOrderNotFoundException(command.orderId()));

		boolean hadActiveStockReservation = order.hasActiveStockReservation();
		SalesOrder cancelled = order.cancel(command.reason());

		if (hadActiveStockReservation) {
			for (var reservationId : order.getStockReservationIds()) {
				releaseStockReservationPort.release(new ReleaseStockReservationCommand(reservationId));
			}
		}

		salesOrderRepositoryPort.save(cancelled);
	}
}
