package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivableRepositoryPort {
	Receivable save(Receivable receivable);

	Optional<Receivable> findById(ReceivableId id);

	/** The receivables already generated for a fiscal document, ordered by installment number. */
	List<Receivable> findByOriginDocumentRef(UUID originDocumentRef);

	/** Every title of the customer, whatever its status. */
	List<Receivable> findByCustomerId(UUID customerId);

	/** The customer's titles that are still to be paid: {@code OPEN} or {@code PARTIALLY_SETTLED}. */
	List<Receivable> findUnsettledByCustomerId(UUID customerId);

	/**
	 * The titles still to be received ({@code OPEN} or {@code PARTIALLY_SETTLED})
	 * that fall due on or before {@code until} and match the company, branch and
	 * bank account of {@code filter}. None when the filter names a cost center,
	 * which receivables are not charged to.
	 */
	List<Receivable> findOutstandingDueUntil(LocalDate until, CashFlowFilter filter);
}
