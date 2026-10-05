package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUserRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		@NotBlank String password,
		@NotNull UUID profileId) {

	public RegisterUserCommand toCommand(UserId callerId) {
		return new RegisterUserCommand(name, email, password, profileId, callerId);
	}
}
