package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.SerialUnitJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialUnitJpaRepository extends JpaRepository<SerialUnitJpaEntity, UUID> {

	List<SerialUnitJpaEntity> findByProductIdAndWarehouseIdAndSerialNumberIn(UUID productId, UUID warehouseId,
			List<String> serialNumbers);
}
