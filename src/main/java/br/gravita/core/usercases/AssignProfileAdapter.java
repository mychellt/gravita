package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.business.SaveCustomProfilePort;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AssignProfileAdapter implements AssignProfilePort {

	private final ProfileRepositoryPort profileRepositoryPort;
	private final SaveCustomProfilePort saveCustomProfilePort;

	public AssignProfileAdapter(final ProfileRepositoryPort profileRepositoryPort, final SaveCustomProfilePort saveCustomProfilePort) {
		this.profileRepositoryPort = profileRepositoryPort;
		this.saveCustomProfilePort = saveCustomProfilePort;
	}

	@Override
	public ProfileDomain execute(final Context context) {
		final ProfileDomain command = context.getData(ProfileDomain.class);
		final Optional<ProfileDomain> existing = profileRepositoryPort.findById(command.getId());

		if (existing.isPresent()) {
			final ProfileDomain profile = existing.get();
			profile.setPermissions(command.getPermissions());
			return profileRepositoryPort.save(profile);
		}

		if (command.getName() == null || command.getName().isBlank()) {
			throw new ResourceNotFoundException("Profile not found: " + command.getId());
		}
		return saveCustomProfilePort.execute(context);
	}
}
