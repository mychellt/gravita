package br.gravita.core.ports.inbound.system;

import br.gravita.core.domain.system.UserId;

/** Guards the platform administration area: only users with the GRAVITA_ADMINISTRATOR profile get through. */
public interface RequireGravitaAdministratorUseCase {

	/** @throws br.gravita.core.domain.exceptions.ForbiddenException when the user is not a Gravita administrator */
	void execute(UserId userId);
}
