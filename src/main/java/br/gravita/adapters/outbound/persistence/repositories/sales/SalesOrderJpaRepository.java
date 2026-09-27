package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderJpaEntity;
import br.gravita.core.domain.sales.SalesOrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderJpaRepository extends JpaRepository<SalesOrderJpaEntity, UUID> {
	List<SalesOrderJpaEntity> findByStatusAndInvoicedAtBetween(SalesOrderStatus status, LocalDate periodStart,
			LocalDate periodEnd);

	List<SalesOrderJpaEntity> findByStatusAndInvoicedAtBetweenAndSalespersonId(SalesOrderStatus status,
			LocalDate periodStart, LocalDate periodEnd, UUID salespersonId);
}
