package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.List;
import java.util.Optional;

public interface PurchaseReceiptRepositoryPort {
	PurchaseReceipt save(PurchaseReceipt purchaseReceipt);
	Optional<PurchaseReceipt> findById(PurchaseReceiptId id);

	List<PurchaseReceipt> findByOrderId(PurchaseOrderId orderId);
}
