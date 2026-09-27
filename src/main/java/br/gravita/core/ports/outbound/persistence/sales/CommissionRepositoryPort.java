package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.Commission;

public interface CommissionRepositoryPort {
	Commission save(Commission commission);
}
