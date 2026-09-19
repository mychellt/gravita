package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class ProfileRepositoryAdapter implements ProfileRepositoryPort {

	private final ProfileJpaRepository jpaRepository;
	private final ProfilePersistenceMapper mapper = new ProfilePersistenceMapper();

	ProfileRepositoryAdapter(ProfileJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<ProfileDomain> findById(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<ProfileDomain> findByName(String name) {
		return jpaRepository.findByName(name).map(mapper::toDomain);
	}

	@Override
	public ProfileDomain save(ProfileDomain profile) {
		ProfileJpaEntity entity = mapper.toEntity(profile);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		ProfileJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
