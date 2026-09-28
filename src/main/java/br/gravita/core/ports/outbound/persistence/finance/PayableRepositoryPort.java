package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayableRepositoryPort {
	Payable save(Payable payable);

	Optional<Payable> findById(PayableId id);

	/** The payables already generated for a purchase receipt, ordered by installment number. */
	List<Payable> findByPurchaseReceiptRef(UUID purchaseReceiptRef);

	/** The payables with the given ids; unknown ids are left out. */
	List<Payable> findByIds(Collection<PayableId> ids);

	/**
	 * The titles still to be paid ({@code OPEN} or {@code APPROVED}) that fall due
	 * on or before {@code until} and match {@code filter}; a cost center matches
	 * a payable whose split charges it.
	 */
	List<Payable> findOutstandingDueUntil(LocalDate until, CashFlowFilter filter);
}
