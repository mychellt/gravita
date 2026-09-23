package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnJpaRepository extends JpaRepository<PurchaseReturnJpaEntity, UUID> {
	List<PurchaseReturnJpaEntity> findByReceiptId(UUID receiptId);
}
