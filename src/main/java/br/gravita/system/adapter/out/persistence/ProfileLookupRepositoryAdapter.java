package br.gravita.system.adapter.out.persistence;

import br.gravita.shared.PersistenceAdapter;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.domain.model.ProfileReference;

import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class ProfileLookupRepositoryAdapter implements ProfileRepositoryPort {

	private final ProfileLookupJpaRepository jpaRepository;

	ProfileLookupRepositoryAdapter(ProfileLookupJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<ProfileReference> findById(UUID profileId) {
		return jpaRepository.findById(profileId).map(entity -> new ProfileReference(entity.getId(), entity.getName()));
	}
}
