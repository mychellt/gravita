package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Receivable;
import java.util.List;
import java.util.UUID;

public interface ReceivableRepositoryPort {
	Receivable save(Receivable receivable);

	/** The receivables already generated for a fiscal document, ordered by installment number. */
	List<Receivable> findByOriginDocumentRef(UUID originDocumentRef);
}
