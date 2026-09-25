package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.SerialUnit;

import java.util.List;
import java.util.UUID;

public interface SerialUnitRepositoryPort {
	List<SerialUnit> saveAll(List<SerialUnit> serialUnits);

	List<SerialUnit> findByProductIdAndWarehouseIdAndSerialNumberIn(UUID productId, UUID warehouseId,
			List<String> serialNumbers);
}
