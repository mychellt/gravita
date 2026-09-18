package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.ProfileDomain;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String name, List<PermissionResponse> permissions) {

	public static ProfileResponse from(ProfileDomain domain) {
		return new ProfileResponse(
				domain.getId(),
				domain.getName(),
				domain.getPermissions().stream().map(PermissionResponse::from).toList());
	}
}
