package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;

import java.util.List;

@UseCase
public class ListProfilesService implements ListProfilesUseCase {

	private final ProfileRepositoryPort profileRepositoryPort;

	public ListProfilesService(ProfileRepositoryPort profileRepositoryPort) {
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public List<ProfileReference> execute() {
		return profileRepositoryPort.findAll();
	}
}
