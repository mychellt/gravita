package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;

/**
 * UC-M2-09: either {@code accessKey} alone (resolved from SEFAZ) or
 * {@code manualData} (typed in full, supplier without XML) must be given -
 * see {@link EnterInboundNfeManuallyUseCase}.
 */
public record EnterInboundNfeManuallyCommand(CompanyId companyId, String accessKey, ManualInboundNfeData manualData) {
}
