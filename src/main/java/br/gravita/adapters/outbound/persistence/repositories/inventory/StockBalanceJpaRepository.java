package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockBalanceJpaRepository extends JpaRepository<StockBalanceJpaEntity, UUID> {

	Optional<StockBalanceJpaEntity> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

	List<StockBalanceJpaEntity> findByProductId(UUID productId);
}
