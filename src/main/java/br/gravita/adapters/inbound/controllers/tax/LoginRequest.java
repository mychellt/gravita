package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthenticateCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {

	public AuthenticateCommand toCommand(String ip, String device) {
		return new AuthenticateCommand(email, password, null, ip, device);
	}
}
