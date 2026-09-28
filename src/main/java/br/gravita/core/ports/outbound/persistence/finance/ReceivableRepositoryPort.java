package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivableRepositoryPort {
	Receivable save(Receivable receivable);

	Optional<Receivable> findById(ReceivableId id);

	/** The receivables already generated for a fiscal document, ordered by installment number. */
	List<Receivable> findByOriginDocumentRef(UUID originDocumentRef);

	/** The customer's titles that are still to be paid: {@code OPEN} or {@code PARTIALLY_SETTLED}. */
	List<Receivable> findUnsettledByCustomerId(UUID customerId);
}
