package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalespersonTarget;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalespersonTargetRepositoryPort {
	SalespersonTarget save(SalespersonTarget target);

	Optional<SalespersonTarget> findBySalespersonAndMonth(UUID salespersonId, YearMonth month);

	/** Every salesperson's target for {@code month}; used by the executive dashboard's company-wide progress. */
	List<SalespersonTarget> findByMonth(YearMonth month);
}
