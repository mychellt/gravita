package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import java.util.Optional;

public interface SalesOrderRepositoryPort {
	SalesOrder save(SalesOrder order);
	Optional<SalesOrder> findById(SalesOrderId id);
}
