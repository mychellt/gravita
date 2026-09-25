package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsQuery;
import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsUseCase;
import br.gravita.core.ports.inbound.inventory.ExpiringLotView;
import br.gravita.core.ports.outbound.inventory.NotifyExpiringLotPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-M5-10. Read-only: never blocks a sale itself, blocking expired-lot
 * allocation is UC-M5-03's responsibility.
 */
@UseCase
public class CheckExpiringLotsService implements CheckExpiringLotsUseCase {

	private final LotRepositoryPort lotRepositoryPort;
	private final NotifyExpiringLotPort notifyExpiringLotPort;

	public CheckExpiringLotsService(LotRepositoryPort lotRepositoryPort, NotifyExpiringLotPort notifyExpiringLotPort) {
		this.lotRepositoryPort = lotRepositoryPort;
		this.notifyExpiringLotPort = notifyExpiringLotPort;
	}

	@Override
	public List<ExpiringLotView> execute(CheckExpiringLotsQuery query) {
		LocalDate cutoffDate = LocalDate.now().plusDays(query.withinDays());

		List<Lot> candidates = query.warehouseId() != null
				? lotRepositoryPort.findByExpiryDateLessThanEqualAndWarehouseId(cutoffDate, query.warehouseId())
				: lotRepositoryPort.findByExpiryDateLessThanEqual(cutoffDate);

		List<ExpiringLotView> expiringLots = candidates.stream()
				.filter(lot -> lot.getQuantity().signum() > 0)
				.map(ExpiringLotView::from)
				.toList();

		notifyExpiringLotPort.notify(expiringLots);
		return expiringLots;
	}
}
