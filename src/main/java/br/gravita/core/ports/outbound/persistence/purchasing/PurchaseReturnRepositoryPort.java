package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import java.util.List;
import java.util.Optional;

public interface PurchaseReturnRepositoryPort {
	PurchaseReturn save(PurchaseReturn purchaseReturn);
	Optional<PurchaseReturn> findById(PurchaseReturnId id);

	/**
	 * UC-M6-09 needs every return already recorded against a receipt to net out
	 * previously-returned quantities before validating a new one against what
	 * was originally received.
	 */
	List<PurchaseReturn> findByReceiptId(PurchaseReceiptId receiptId);
}
