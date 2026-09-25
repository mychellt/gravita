package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.LotJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotJpaRepository extends JpaRepository<LotJpaEntity, UUID> {

	Optional<LotJpaEntity> findByProductIdAndWarehouseIdAndCode(UUID productId, UUID warehouseId, String code);

	List<LotJpaEntity> findByExpiryDateLessThanEqual(LocalDate cutoffDate);

	List<LotJpaEntity> findByExpiryDateLessThanEqualAndWarehouseId(LocalDate cutoffDate, UUID warehouseId);
}
