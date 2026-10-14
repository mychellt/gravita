package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.List;
import java.util.Optional;

public interface TransportModalityRepositoryPort extends BaseRepositoryPort<TransportModalityDomain> {
	List<TransportModalityDomain> findAllActive();

	Optional<TransportModalityDomain> findActiveByCode(final String code);
}
