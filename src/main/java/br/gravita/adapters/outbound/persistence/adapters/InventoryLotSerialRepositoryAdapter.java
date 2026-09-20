package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.ports.outbound.persistence.InventoryLotSerialRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** M5 (Estoque) does not exist yet, so there are no lot/serial records to open; replace with a real inventory query once M5 lands. */
@Component
class InventoryLotSerialRepositoryAdapter implements InventoryLotSerialRepositoryPort {

	@Override
	public boolean hasOpenLotsOrSerials(UUID productId) {
		return false;
	}
}
