package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepositoryPort {
	SalesOrder save(SalesOrder order);
	Optional<SalesOrder> findById(SalesOrderId id);
	List<SalesOrder> findInvoicedByPeriod(LocalDate periodStart, LocalDate periodEnd);
	List<SalesOrder> findInvoicedByPeriodAndSalesperson(LocalDate periodStart, LocalDate periodEnd,
			UUID salespersonId);
}
