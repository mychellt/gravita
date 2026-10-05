package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.ProfilePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.ProfileJpaRepository;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class ProfileRepositoryAdapter implements ProfileRepositoryPort {

	private final ProfileJpaRepository jpaRepository;
	private final ProfilePersistenceMapper mapper;

	ProfileRepositoryAdapter(final ProfileJpaRepository jpaRepository, final ProfilePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<ProfileDomain> findById(final UUID id) {
		return jpaRepository.findById(id).map(mapper::map);
	}

	@Override
	public Optional<ProfileDomain> findByName(final String name) {
		return jpaRepository.findByName(name).map(mapper::map);
	}

	@Override
	public ProfileDomain save(final ProfileDomain profile) {
		final ProfileJpaEntity entity = mapper.map(profile);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final ProfileJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}
}
