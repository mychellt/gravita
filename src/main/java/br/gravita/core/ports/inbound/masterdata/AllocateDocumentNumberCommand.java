package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;

public record AllocateDocumentNumberCommand(CompanyId companyId, FiscalDocumentType documentType) {
}
