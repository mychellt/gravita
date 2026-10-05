package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestJpaRepository extends JpaRepository<PurchaseRequestJpaEntity, UUID> {

	boolean existsByOriginAndStatusAndItemsProductId(PurchaseRequestOrigin origin, PurchaseRequestStatus status,
			UUID productId);
}
