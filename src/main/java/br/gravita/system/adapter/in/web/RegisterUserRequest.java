package br.gravita.system.adapter.in.web;

import br.gravita.system.application.port.in.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUserRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		@NotBlank String password,
		@NotNull UUID profileId) {

	public RegisterUserCommand toCommand() {
		return new RegisterUserCommand(name, email, password, profileId);
	}
}
