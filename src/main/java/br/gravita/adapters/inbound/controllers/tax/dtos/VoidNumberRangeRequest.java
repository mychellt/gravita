package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoidNumberRangeRequest(@NotNull UUID companyId, @NotBlank String series, @NotNull Long startNumber,
		@NotNull Long endNumber, @NotBlank String justification) {

	public VoidNumberRangeCommand toCommand() {
		return new VoidNumberRangeCommand(CompanyId.of(companyId), series, startNumber, endNumber, justification);
	}
}
