package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.system.UserId;

import java.util.UUID;

public record UserResponse(UUID id) {

	public static UserResponse from(UserId id) {
		return new UserResponse(id.value());
	}
}
