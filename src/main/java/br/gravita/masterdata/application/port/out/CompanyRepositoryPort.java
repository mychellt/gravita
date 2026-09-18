package br.gravita.masterdata.application.port.out;

import br.gravita.masterdata.domain.model.Company;
import br.gravita.masterdata.domain.model.CompanyId;

import java.util.Optional;

public interface CompanyRepositoryPort {
	Company save(Company company);
	Optional<Company> findById(CompanyId id);
}
