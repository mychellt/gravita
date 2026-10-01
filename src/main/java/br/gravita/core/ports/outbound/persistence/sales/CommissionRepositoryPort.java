package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.SalesOrderId;
import java.util.Collection;
import java.util.List;

public interface CommissionRepositoryPort {
	Commission save(Commission commission);
	List<Commission> findByOrderIds(Collection<SalesOrderId> orderIds);
}
