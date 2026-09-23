package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.List;
import java.util.Optional;

public interface PurchaseReceiptRepositoryPort {
	PurchaseReceipt save(PurchaseReceipt purchaseReceipt);
	Optional<PurchaseReceipt> findById(PurchaseReceiptId id);

	/**
	 * UC-M6-08 needs every receipt already confirmed against an order (plus the
	 * one it is currently confirming) to decide whether the order's items are
	 * now fully covered.
	 */
	List<PurchaseReceipt> findByOrderId(PurchaseOrderId orderId);
}
