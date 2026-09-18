package br.gravita.masterdata.application.port.out;

import br.gravita.masterdata.domain.model.DocumentSeries;

public interface DocumentSeriesRepositoryPort {
	DocumentSeries save(DocumentSeries documentSeries);
}
