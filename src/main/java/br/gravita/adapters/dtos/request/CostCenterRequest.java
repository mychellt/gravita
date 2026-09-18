package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.CostCenterDomain;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CostCenterRequest(@NotBlank String code, @NotBlank String name, UUID parentId) {

	public CostCenterDomain toDomain(UUID id) {
		return CostCenterDomain.builder()
				.id(id)
				.code(code)
				.name(name)
				.parentId(parentId)
				.build();
	}
}
