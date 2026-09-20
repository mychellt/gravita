package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.ports.business.SaveCustomProfilePort;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * SaveCustomProfileUseCase (M10-04): saves an arbitrary module/screen/action permission
 * combination as a new, named, reusable {@link ProfileDomain}, selectable exactly like a
 * standard profile. Reached via {@code PUT /api/profiles/{id}/permissions} - the same
 * shared endpoint as AssignProfileUseCase (M10-03, see {@link AssignProfileAdapter}), which
 * delegates here when the path {@code id} has no matching profile.
 */
@Component
public class SaveCustomProfileAdapter implements SaveCustomProfilePort {

	private final ProfileRepositoryPort profileRepositoryPort;

	public SaveCustomProfileAdapter(ProfileRepositoryPort profileRepositoryPort) {
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public ProfileDomain execute(Context context) {
		ProfileDomain command = context.getData(ProfileDomain.class);
		ensureNameIsUnique(command.getName());
		return profileRepositoryPort.save(command);
	}

	private void ensureNameIsUnique(String name) {
		profileRepositoryPort.findByName(name).ifPresent(profile -> {
			throw new DuplicateResourceException("Profile name already in use: " + name);
		});
	}
}
