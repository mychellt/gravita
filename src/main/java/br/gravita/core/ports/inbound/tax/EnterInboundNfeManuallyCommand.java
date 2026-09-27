package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;

public record EnterInboundNfeManuallyCommand(CompanyId companyId, String accessKey, ManualInboundNfeData manualData) {
}
