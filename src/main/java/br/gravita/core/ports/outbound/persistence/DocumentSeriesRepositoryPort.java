package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;

import java.util.Optional;

public interface DocumentSeriesRepositoryPort {
	DocumentSeries save(DocumentSeries documentSeries);
	Optional<DocumentSeries> findByCompanyIdAndDocumentType(CompanyId companyId, FiscalDocumentType documentType);
}
