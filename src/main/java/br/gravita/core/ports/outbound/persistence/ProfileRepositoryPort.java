package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.ProfileDomain;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepositoryPort {
	ProfileDomain save(final ProfileDomain profile);
	Optional<ProfileDomain> findById(final UUID id);
	Optional<ProfileDomain> findByName(final String name);
}
