package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.adapters.outbound.persistence.repositories.tax.ProfileLookupJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class ProfileLookupRepositoryAdapter implements ProfileRepositoryPort {

	private final ProfileLookupJpaRepository jpaRepository;

	ProfileLookupRepositoryAdapter(final ProfileLookupJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<ProfileReference> findById(final UUID profileId) {
		return jpaRepository.findById(profileId).map(entity -> new ProfileReference(entity.getId(), entity.getName()));
	}

	@Override
	public Optional<ProfileReference> findByName(final String name) {
		return jpaRepository.findByName(name).map(entity -> new ProfileReference(entity.getId(), entity.getName()));
	}

	@Override
	public List<ProfileReference> findAll() {
		return jpaRepository.findAll().stream()
				.map(entity -> new ProfileReference(entity.getId(), entity.getName()))
				.toList();
	}
}
