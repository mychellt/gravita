package br.gravita.core.ports.persistence;

import java.util.UUID;

public interface InventoryLotSerialRepositoryPort {
	boolean hasOpenLotsOrSerials(UUID productId);
}
