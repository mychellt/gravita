package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayableRepositoryPort {
	Payable save(Payable payable);

	Optional<Payable> findById(PayableId id);

	/** The payables already generated for a purchase receipt, ordered by installment number. */
	List<Payable> findByPurchaseReceiptRef(UUID purchaseReceiptRef);
}
