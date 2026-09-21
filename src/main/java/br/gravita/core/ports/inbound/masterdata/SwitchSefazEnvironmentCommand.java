package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;

public record SwitchSefazEnvironmentCommand(CompanyId companyId, SefazEnvironment environment) {
}
