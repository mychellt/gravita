package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthenticateCommand;
import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(@NotBlank String email, @NotBlank String password, @NotBlank String totpCode) {

	public AuthenticateCommand toCommand(String ip, String device) {
		return new AuthenticateCommand(email, password, totpCode, ip, device);
	}
}
