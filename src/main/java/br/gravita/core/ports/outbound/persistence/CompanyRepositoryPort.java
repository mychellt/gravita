package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;

import java.util.Optional;

public interface CompanyRepositoryPort {
	Company save(Company company);
	Optional<Company> findById(CompanyId id);

	boolean existsByCnpj(String cnpj);
}
