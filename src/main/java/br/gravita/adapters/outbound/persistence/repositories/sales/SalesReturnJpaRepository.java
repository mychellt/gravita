package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnJpaRepository extends JpaRepository<SalesReturnJpaEntity, UUID> {
	List<SalesReturnJpaEntity> findBySalesOrderId(UUID salesOrderId);
}
