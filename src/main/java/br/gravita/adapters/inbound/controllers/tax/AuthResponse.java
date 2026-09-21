package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthStatus;

public record AuthResponse(AuthStatus status, String sessionToken) {

	public static AuthResponse from(AuthResult result) {
		return new AuthResponse(result.status(), result.sessionToken());
	}
}
