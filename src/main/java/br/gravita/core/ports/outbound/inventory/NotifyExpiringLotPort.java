package br.gravita.core.ports.outbound.inventory;

import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsUseCase;
import br.gravita.core.ports.inbound.inventory.ExpiringLotView;

import java.util.List;

/**
 * Pushes {@link CheckExpiringLotsUseCase} results to the M9 "estoque crítico"
 * dashboard widget and M10's alerting pipeline (UC-M5-10, AC3), so those
 * consumers don't need to poll the query endpoint directly.
 */
public interface NotifyExpiringLotPort {

	void notify(List<ExpiringLotView> expiringLots);
}
