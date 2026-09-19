package br.gravita.system.application.port.out;

import br.gravita.system.domain.model.ProfileReference;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepositoryPort {

	Optional<ProfileReference> findById(UUID profileId);
}
