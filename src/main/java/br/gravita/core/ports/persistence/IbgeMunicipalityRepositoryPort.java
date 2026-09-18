package br.gravita.core.ports.persistence;

import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.persistence.commons.ReadRepositoryPort;

import java.util.List;
import java.util.Optional;

public interface IbgeMunicipalityRepositoryPort extends ReadRepositoryPort<IbgeMunicipalityDomain> {
	Optional<IbgeMunicipalityDomain> findByIbgeCode(final String ibgeCode);
	List<IbgeMunicipalityDomain> saveAll(final List<IbgeMunicipalityDomain> municipalities);
}
