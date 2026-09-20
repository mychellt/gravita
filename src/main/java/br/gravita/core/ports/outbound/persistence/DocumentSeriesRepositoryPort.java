package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.DocumentSeries;

public interface DocumentSeriesRepositoryPort {
	DocumentSeries save(DocumentSeries documentSeries);
}
