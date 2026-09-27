package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepositoryPort {
	PurchaseRequest save(PurchaseRequest purchaseRequest);
	Optional<PurchaseRequest> findById(PurchaseRequestId id);

	boolean existsOpenByOriginAndProductId(PurchaseRequestOrigin origin, UUID productId);
}
