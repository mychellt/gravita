package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.application.port.out.CompanyRepositoryPort;
import br.gravita.masterdata.domain.model.Company;
import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.shared.PersistenceAdapter;

import java.util.Optional;

@PersistenceAdapter
class CompanyRepositoryAdapter implements CompanyRepositoryPort {

	private final CompanyJpaRepository jpaRepository;
	private final CompanyPersistenceMapper mapper = new CompanyPersistenceMapper();

	CompanyRepositoryAdapter(CompanyJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Company save(Company company) {
		CompanyJpaEntity saved = jpaRepository.save(mapper.toEntity(company));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Company> findById(CompanyId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
