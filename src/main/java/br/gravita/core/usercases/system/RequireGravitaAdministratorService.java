package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.system.RequireGravitaAdministratorUseCase;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;

@UseCase
public class RequireGravitaAdministratorService implements RequireGravitaAdministratorUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public RequireGravitaAdministratorService(final UserRepositoryPort userRepositoryPort,
			final ProfileRepositoryPort profileRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public void execute(final UserId userId) {
		final boolean allowed = userRepositoryPort.findById(userId)
				.flatMap(user -> profileRepositoryPort.findById(user.getProfileId()))
				.map(ProfileReference::isGravitaAdministrator)
				.orElse(false);
		if (!allowed) {
			throw new ForbiddenException("Only Gravita administrators can access the administrative area");
		}
	}
}
