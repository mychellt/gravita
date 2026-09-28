package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalespersonTarget;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public interface SalespersonTargetRepositoryPort {
	SalespersonTarget save(SalespersonTarget target);

	Optional<SalespersonTarget> findBySalespersonAndMonth(UUID salespersonId, YearMonth month);
}
