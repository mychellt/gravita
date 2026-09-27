package br.gravita.core.ports.outbound.inventory;

import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsUseCase;
import br.gravita.core.ports.inbound.inventory.ExpiringLotView;

import java.util.List;

public interface NotifyExpiringLotPort {

	void notify(List<ExpiringLotView> expiringLots);
}
