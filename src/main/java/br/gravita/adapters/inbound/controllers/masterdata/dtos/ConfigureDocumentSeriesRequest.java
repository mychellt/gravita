package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfigureDocumentSeriesRequest(
		@NotBlank String series,
		@NotNull @Positive Long nextNumber) {

	public ConfigureDocumentSeriesCommand toCommand(final CompanyId companyId, final FiscalDocumentType documentType) {
		return new ConfigureDocumentSeriesCommand(companyId, documentType, series, nextNumber);
	}
}
