package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBalanceJpaRepository extends JpaRepository<StockBalanceJpaEntity, UUID> {

	Optional<StockBalanceJpaEntity> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

	List<StockBalanceJpaEntity> findByProductId(UUID productId);

	List<StockBalanceJpaEntity> findByWarehouseId(UUID warehouseId);

	@Query(value = """
			select b from StockBalanceJpaEntity b, ProductJpaEntity p
			where p.id = b.productId
			and b.warehouseId = :warehouseId
			and (:productId is null or b.productId = :productId)
			order by p.internalCode, b.productId
			""", countQuery = """
			select count(b) from StockBalanceJpaEntity b, ProductJpaEntity p
			where p.id = b.productId
			and b.warehouseId = :warehouseId
			and (:productId is null or b.productId = :productId)
			""")
	Page<StockBalanceJpaEntity> search(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId,
			Pageable pageable);
}
