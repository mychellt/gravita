package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@UseCase
public class AllocateDocumentNumberService implements AllocateDocumentNumberUseCase {

	private static final int MAX_ATTEMPTS = 10;

	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	public AllocateDocumentNumberService(final DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public DocumentNumber execute(final AllocateDocumentNumberCommand command) {
		ObjectOptimisticLockingFailureException lastConflict = null;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			try {
				return allocate(command);
			} catch (final ObjectOptimisticLockingFailureException conflict) {
				lastConflict = conflict;
			}
		}
		throw lastConflict;
	}

	private DocumentNumber allocate(final AllocateDocumentNumberCommand command) {
		final DocumentSeries existing = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(command.companyId(), command.documentType())
				.orElseThrow(() -> new DocumentSeriesNotFoundException(command.companyId().value(), command.documentType()));

		final String series = existing.getSeries();
		final Long allocatedNumber = existing.getNextNumber();

		documentSeriesRepositoryPort.save(existing.allocateNext());

		return new DocumentNumber(series, allocatedNumber);
	}
}
