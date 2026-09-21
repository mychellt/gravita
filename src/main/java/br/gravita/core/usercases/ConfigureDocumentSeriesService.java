package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesUseCase;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;

/**
 * A {@link DocumentSeries} row for every {@code (company, documentType)} pair
 * already exists as a placeholder created at company registration (UC-01),
 * so this only ever reconfigures an existing row - never creates one (UC-M1-04).
 */
@UseCase
public class ConfigureDocumentSeriesService implements ConfigureDocumentSeriesUseCase {

	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	public ConfigureDocumentSeriesService(DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public void execute(ConfigureDocumentSeriesCommand command) {
		DocumentSeries existing = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(command.companyId(), command.documentType())
				.orElseThrow(() -> new DocumentSeriesNotFoundException(command.companyId().value(), command.documentType()));

		DocumentSeries reconfigured = existing.reconfigure(command.series(), command.nextNumber());
		documentSeriesRepositoryPort.save(reconfigured);
	}
}
