package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;

import java.util.List;
import java.util.UUID;

public interface SerialUnitRepositoryPort {
	List<SerialUnit> saveAll(List<SerialUnit> serialUnits);

	List<SerialUnit> findByProductIdAndWarehouseIdAndSerialNumberIn(UUID productId, UUID warehouseId,
			List<String> serialNumbers);

	/** The warehouse's serial units, optionally of one product, ordered by product then serial number. */
	Page<SerialUnit> search(UUID warehouseId, UUID productId, PageQuery pageQuery);
}
