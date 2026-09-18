package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.CostCenterDomain;

import java.util.UUID;

public record CostCenterResponse(UUID id, String code, String name, UUID parentId) {

	public static CostCenterResponse from(CostCenterDomain domain) {
		return new CostCenterResponse(domain.getId(), domain.getCode(), domain.getName(), domain.getParentId());
	}
}
