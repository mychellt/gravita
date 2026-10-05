package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.usercases.system.CurrentUser;

import java.util.UUID;

public record CurrentUserResponse(String name, String email, String profile, UUID companyId) {

	public static CurrentUserResponse from(CurrentUser user) {
		return new CurrentUserResponse(user.name(), user.email(), user.profile(), user.companyId());
	}
}
