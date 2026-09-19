package br.gravita.system.adapter.in.web;

import br.gravita.system.application.port.in.UpdateUserCommand;
import br.gravita.system.domain.model.UserStatus;
import jakarta.validation.constraints.Email;

import java.util.UUID;

public record UpdateUserRequest(String name, @Email String email, UUID profileId, UserStatus status) {

	public UpdateUserCommand toCommand(UUID userId) {
		return new UpdateUserCommand(userId, name, email, profileId, status);
	}
}
