package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnId;
import java.util.List;
import java.util.Optional;

public interface SalesReturnRepositoryPort {
	SalesReturn save(SalesReturn salesReturn);
	Optional<SalesReturn> findById(SalesReturnId id);

	List<SalesReturn> findByOrderId(SalesOrderId orderId);
}
