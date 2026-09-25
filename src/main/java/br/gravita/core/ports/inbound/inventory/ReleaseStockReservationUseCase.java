package br.gravita.core.ports.inbound.inventory;

public interface ReleaseStockReservationUseCase {
	void execute(ReleaseStockReservationCommand command);
}
