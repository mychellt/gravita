package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.ports.outbound.persistence.InventoryLotSerialRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class InventoryLotSerialRepositoryAdapter implements InventoryLotSerialRepositoryPort {

	@Override
	public boolean hasOpenLotsOrSerials(final UUID productId) {
		return false;
	}
}
