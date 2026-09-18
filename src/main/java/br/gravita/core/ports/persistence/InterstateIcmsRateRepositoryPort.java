package br.gravita.core.ports.persistence;

import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.persistence.commons.ReadRepositoryPort;

import java.util.List;
import java.util.Optional;

public interface InterstateIcmsRateRepositoryPort extends ReadRepositoryPort<InterstateIcmsRateDomain> {
	Optional<InterstateIcmsRateDomain> findByOriginStateAndDestinationState(final String originState, final String destinationState);
	List<InterstateIcmsRateDomain> saveAll(final List<InterstateIcmsRateDomain> rates);
}
