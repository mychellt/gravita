package br.gravita.core.ports.outbound.persistence.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepositoryPort {
	PurchaseRequest save(PurchaseRequest purchaseRequest);
	Optional<PurchaseRequest> findById(PurchaseRequestId id);

	/**
	 * Used by the M5-11 auto-reorder trigger (GRA-88) to avoid opening a
	 * second {@code MIN_STOCK_TRIGGER} request for a product that already has
	 * one OPEN. {@code PurchaseRequest} has no warehouse of its own yet (only
	 * its items carry a product), so this checks by product across all
	 * warehouses.
	 */
	boolean existsOpenByOriginAndProductId(PurchaseRequestOrigin origin, UUID productId);
}
