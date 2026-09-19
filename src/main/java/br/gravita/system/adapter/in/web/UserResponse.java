package br.gravita.system.adapter.in.web;

import br.gravita.system.domain.model.UserId;

import java.util.UUID;

public record UserResponse(UUID id) {

	public static UserResponse from(UserId id) {
		return new UserResponse(id.value());
	}
}
