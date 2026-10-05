package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.CurrentUser;

public record CurrentUserResponse(String name, String email, String profile) {

	public static CurrentUserResponse from(CurrentUser user) {
		return new CurrentUserResponse(user.name(), user.email(), user.profile());
	}
}
