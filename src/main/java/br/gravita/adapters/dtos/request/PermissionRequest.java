package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PermissionRequest(
		@NotBlank String module,
		@NotBlank String screen,
		@NotNull PermissionAction action) {

	public PermissionDomain toDomain() {
		return PermissionDomain.builder()
				.module(module)
				.screen(screen)
				.action(action)
				.build();
	}
}
