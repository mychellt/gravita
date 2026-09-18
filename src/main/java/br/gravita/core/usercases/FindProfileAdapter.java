package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindProfilePort;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindProfileAdapter implements FindProfilePort {

	private final ProfileRepositoryPort profileRepositoryPort;

	public FindProfileAdapter(ProfileRepositoryPort profileRepositoryPort) {
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public ProfileDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		return profileRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + id));
	}
}
