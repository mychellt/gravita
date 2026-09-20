package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.CompanyId;

import java.util.UUID;

public record CompanyResponse(UUID id) {
	public static CompanyResponse from(CompanyId id) {
		return new CompanyResponse(id.value());
	}
}
