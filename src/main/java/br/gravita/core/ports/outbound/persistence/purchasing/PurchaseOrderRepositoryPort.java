package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import java.util.Optional;

public interface PurchaseOrderRepositoryPort {
	PurchaseOrder save(PurchaseOrder purchaseOrder);
	Optional<PurchaseOrder> findById(PurchaseOrderId id);
}
