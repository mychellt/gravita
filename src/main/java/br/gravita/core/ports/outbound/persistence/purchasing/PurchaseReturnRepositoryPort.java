package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import java.util.List;
import java.util.Optional;

public interface PurchaseReturnRepositoryPort {
	PurchaseReturn save(PurchaseReturn purchaseReturn);
	Optional<PurchaseReturn> findById(PurchaseReturnId id);

	List<PurchaseReturn> findByReceiptId(PurchaseReceiptId receiptId);
}
