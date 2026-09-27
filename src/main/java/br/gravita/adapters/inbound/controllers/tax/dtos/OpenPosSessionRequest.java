package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.OpenPosSessionCommand;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OpenPosSessionRequest(@NotNull UUID registerId, @NotNull UUID operatorId, @NotNull UUID companyId,
		@NotNull BigDecimal openingChangeAmount) {

	public OpenPosSessionCommand toCommand() {
		return new OpenPosSessionCommand(registerId, operatorId, CompanyId.of(companyId), openingChangeAmount);
	}
}
