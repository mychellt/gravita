package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptJpaRepository extends JpaRepository<PurchaseReceiptJpaEntity, UUID> {
	List<PurchaseReceiptJpaEntity> findByOrderId(UUID orderId);
}
