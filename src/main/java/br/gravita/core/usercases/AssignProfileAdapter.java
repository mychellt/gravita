package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.business.SaveCustomProfilePort;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Backs {@code PUT /api/profiles/{id}/permissions} for standard profiles (shared REST
 * endpoint per docs/specs/m10-sistema.md): replaces the permission set of an existing
 * profile (AssignProfileUseCase, M10-03). When {@code id} has no matching profile and a
 * {@code name} is supplied, delegates to {@link SaveCustomProfilePort} to create a new
 * custom profile instead (SaveCustomProfileUseCase, M10-04).
 */
@Component
public class AssignProfileAdapter implements AssignProfilePort {

	private final ProfileRepositoryPort profileRepositoryPort;
	private final SaveCustomProfilePort saveCustomProfilePort;

	public AssignProfileAdapter(ProfileRepositoryPort profileRepositoryPort, SaveCustomProfilePort saveCustomProfilePort) {
		this.profileRepositoryPort = profileRepositoryPort;
		this.saveCustomProfilePort = saveCustomProfilePort;
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
		return saveCustomProfilePort.execute(context);
	}
}
