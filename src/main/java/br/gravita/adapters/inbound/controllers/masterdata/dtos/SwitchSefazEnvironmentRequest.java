package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentCommand;
import jakarta.validation.constraints.NotNull;

public record SwitchSefazEnvironmentRequest(@NotNull SefazEnvironment environment) {

	public SwitchSefazEnvironmentCommand toCommand(final CompanyId companyId) {
		return new SwitchSefazEnvironmentCommand(companyId, environment);
	}
}
