package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.ports.business.SaveCustomProfilePort;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

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
