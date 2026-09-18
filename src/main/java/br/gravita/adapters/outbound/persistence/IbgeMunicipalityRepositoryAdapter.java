package br.gravita.adapters.outbound.persistence;

import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.persistence.IbgeMunicipalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class IbgeMunicipalityRepositoryAdapter implements IbgeMunicipalityRepositoryPort {

	private final IbgeMunicipalityJpaRepository jpaRepository;
	private final IbgeMunicipalityPersistenceMapper mapper = new IbgeMunicipalityPersistenceMapper();

	IbgeMunicipalityRepositoryAdapter(IbgeMunicipalityJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<IbgeMunicipalityDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<IbgeMunicipalityDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<IbgeMunicipalityDomain> findByIbgeCode(String ibgeCode) {
		return jpaRepository.findByIbgeCode(ibgeCode).map(mapper::toDomain);
	}

	@Override
	public List<IbgeMunicipalityDomain> saveAll(List<IbgeMunicipalityDomain> municipalities) {
		return jpaRepository.saveAll(municipalities.stream().map(mapper::toEntity).toList())
				.stream().map(mapper::toDomain).toList();
	}
}
