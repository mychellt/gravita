package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.PlanDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepositoryPort {
	PlanDomain save(final PlanDomain plan);
	Optional<PlanDomain> findById(final UUID id);
	List<PlanDomain> findAll();
	boolean hasSubscriptions(final UUID id);
	void deleteById(final UUID id);
}
