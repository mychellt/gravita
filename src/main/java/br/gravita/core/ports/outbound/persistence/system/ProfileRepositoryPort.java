package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.ProfileReference;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepositoryPort {

	Optional<ProfileReference> findById(UUID profileId);

	Optional<ProfileReference> findByName(String name);
}
