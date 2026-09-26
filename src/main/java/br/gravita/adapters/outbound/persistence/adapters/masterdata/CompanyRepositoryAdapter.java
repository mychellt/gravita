package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.CompanyPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.CompanyJpaRepository;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.annotations.PersistenceAdapter;

import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class CompanyRepositoryAdapter implements CompanyRepositoryPort {

	private final CompanyJpaRepository jpaRepository;
	private final CompanyPersistenceMapper mapper;

	CompanyRepositoryAdapter(CompanyJpaRepository jpaRepository, CompanyPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Company save(Company company) {
		CompanyJpaEntity entity = mapper.toEntity(company);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		CompanyJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Company> findById(CompanyId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public List<Company> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}
}
