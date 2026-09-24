package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;

public record ImportSupplierNfeXmlCommand(CompanyId companyId, byte[] xmlFile) {
}
