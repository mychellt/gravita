package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.ProfileDomain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignProfilePermissionsRequest(String name, @NotEmpty List<@Valid PermissionRequest> permissions) {

	public ProfileDomain toDomain(final UUID profileId) {
		return ProfileDomain.builder()
				.id(profileId)
				.name(name)
				.permissions(permissions.stream().map(PermissionRequest::toDomain).toList())
				.build();
	}
}
