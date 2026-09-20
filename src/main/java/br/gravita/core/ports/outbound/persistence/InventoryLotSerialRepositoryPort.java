package br.gravita.core.ports.outbound.persistence;

import java.util.UUID;

public interface InventoryLotSerialRepositoryPort {
	boolean hasOpenLotsOrSerials(UUID productId);
}
