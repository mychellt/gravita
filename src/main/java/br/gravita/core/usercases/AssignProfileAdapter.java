package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Backs {@code PUT /api/profiles/{id}/permissions} for both standard and custom profiles
 * (shared REST endpoint per docs/specs/m10-sistema.md): replaces the permission set of an
 * existing profile (AssignProfileUseCase, M10-03), or - when {@code id} has no matching
 * profile and a {@code name} is supplied - creates a new custom profile with a unique name
 * (SaveCustomProfileUseCase, M10-04).
 */
@Component
public class AssignProfileAdapter implements AssignProfilePort {

	private final ProfileRepositoryPort profileRepositoryPort;

	public AssignProfileAdapter(ProfileRepositoryPort profileRepositoryPort) {
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public ProfileDomain execute(Context context) {
		ProfileDomain command = context.getData(ProfileDomain.class);
		Optional<ProfileDomain> existing = profileRepositoryPort.findById(command.getId());

		if (existing.isPresent()) {
			ProfileDomain profile = existing.get();
			profile.setPermissions(command.getPermissions());
			return profileRepositoryPort.save(profile);
		}

		if (command.getName() == null || command.getName().isBlank()) {
			throw new ResourceNotFoundException("Profile not found: " + command.getId());
		}
		ensureNameIsUnique(command.getName());
		return profileRepositoryPort.save(command);
	}

	private void ensureNameIsUnique(String name) {
		profileRepositoryPort.findByName(name).ifPresent(profile -> {
			throw new DuplicateResourceException("Profile name already in use: " + name);
		});
	}
}
