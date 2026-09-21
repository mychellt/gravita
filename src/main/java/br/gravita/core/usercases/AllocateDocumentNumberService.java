package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * UC-15: reserves the next number in a company's document series for `tax`
 * (M2/M3/M4) right before a fiscal document is queued for transmission.
 * Concurrent callers race on the row's optimistic-lock version instead of a
 * table-wide lock (doc §13: never block the caller waiting on a response), so
 * a losing attempt is retried here against a fresh read rather than surfaced
 * to the caller as a conflict.
 */
@UseCase
public class AllocateDocumentNumberService implements AllocateDocumentNumberUseCase {

	private static final int MAX_ATTEMPTS = 10;

	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	public AllocateDocumentNumberService(DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public DocumentNumber execute(AllocateDocumentNumberCommand command) {
		ObjectOptimisticLockingFailureException lastConflict = null;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			try {
				return allocate(command);
			} catch (ObjectOptimisticLockingFailureException conflict) {
				lastConflict = conflict;
			}
		}
		throw lastConflict;
	}

	private DocumentNumber allocate(AllocateDocumentNumberCommand command) {
		DocumentSeries existing = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(command.companyId(), command.documentType())
				.orElseThrow(() -> new DocumentSeriesNotFoundException(command.companyId().value(), command.documentType()));

		String series = existing.getSeries();
		Long allocatedNumber = existing.getNextNumber();

		documentSeriesRepositoryPort.save(existing.allocateNext());

		return new DocumentNumber(series, allocatedNumber);
	}
}
