package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort extends BaseRepositoryPort<CustomerDomain> {

	List<CustomerDomain> findAllByCompanyId(UUID companyId);

	Optional<CustomerDomain> findByIdAndCompanyId(UUID id, UUID companyId);
}
