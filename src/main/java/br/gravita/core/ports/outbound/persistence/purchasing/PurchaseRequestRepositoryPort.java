package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.Optional;

public interface PurchaseRequestRepositoryPort {
	PurchaseRequest save(PurchaseRequest purchaseRequest);
	Optional<PurchaseRequest> findById(PurchaseRequestId id);
}
