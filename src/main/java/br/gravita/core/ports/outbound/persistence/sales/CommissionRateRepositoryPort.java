package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.CommissionRate;
import java.util.Optional;
import java.util.UUID;

public interface CommissionRateRepositoryPort {
	Optional<CommissionRate> findBySalespersonAndProduct(UUID salespersonId, UUID productId);
}
