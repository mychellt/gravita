package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.core.domain.shared.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.adapters.outbound.persistence.repositories.tax.ProfileLookupJpaRepository;

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
