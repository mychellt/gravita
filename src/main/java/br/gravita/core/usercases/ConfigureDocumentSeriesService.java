package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesUseCase;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;

@UseCase
public class ConfigureDocumentSeriesService implements ConfigureDocumentSeriesUseCase {

	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	public ConfigureDocumentSeriesService(final DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public void execute(final ConfigureDocumentSeriesCommand command) {
		final DocumentSeries existing = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(command.companyId(), command.documentType())
				.orElseThrow(() -> new DocumentSeriesNotFoundException(command.companyId().value(), command.documentType()));

		final DocumentSeries reconfigured = existing.reconfigure(command.series(), command.nextNumber());
		documentSeriesRepositoryPort.save(reconfigured);
	}
}
