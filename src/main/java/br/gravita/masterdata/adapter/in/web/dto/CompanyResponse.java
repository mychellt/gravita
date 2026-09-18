package br.gravita.masterdata.adapter.in.web.dto;

import br.gravita.masterdata.domain.model.CompanyId;

import java.util.UUID;

public record CompanyResponse(UUID id) {
	public static CompanyResponse from(CompanyId id) {
		return new CompanyResponse(id.value());
	}
}
