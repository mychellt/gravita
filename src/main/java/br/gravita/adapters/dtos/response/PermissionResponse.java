package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;

public record PermissionResponse(String module, String screen, PermissionAction action) {

	public static PermissionResponse from(final PermissionDomain domain) {
		return new PermissionResponse(domain.getModule(), domain.getScreen(), domain.getAction());
	}
}
