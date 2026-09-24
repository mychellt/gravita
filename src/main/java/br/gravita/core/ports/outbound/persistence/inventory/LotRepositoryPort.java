package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.Lot;

import java.util.Optional;
import java.util.UUID;

public interface LotRepositoryPort {

	Optional<Lot> findByProductIdAndWarehouseIdAndCode(UUID productId, UUID warehouseId, String code);

	Lot save(Lot lot);
}
