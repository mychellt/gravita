package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionJpaRepository extends JpaRepository<CommissionJpaEntity, UUID> {
	List<CommissionJpaEntity> findBySalesOrderIdIn(Collection<UUID> salesOrderIds);
}
