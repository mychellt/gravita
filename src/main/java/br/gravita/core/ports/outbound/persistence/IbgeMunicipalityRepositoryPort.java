package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.outbound.persistence.commons.ReadRepositoryPort;

import java.util.List;
import java.util.Optional;

public interface IbgeMunicipalityRepositoryPort extends ReadRepositoryPort<IbgeMunicipalityDomain> {
	Optional<IbgeMunicipalityDomain> findByIbgeCode(final String ibgeCode);
	List<IbgeMunicipalityDomain> saveAll(final List<IbgeMunicipalityDomain> municipalities);
}
