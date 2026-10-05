package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.UpdateUserCommand;
import br.gravita.core.domain.system.UserStatus;
import jakarta.validation.constraints.Email;

import java.util.UUID;

public record UpdateUserRequest(String name, @Email String email, UUID profileId, UserStatus status) {

	public UpdateUserCommand toCommand(final UUID userId) {
		return new UpdateUserCommand(userId, name, email, profileId, status);
	}
}
