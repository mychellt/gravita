package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderJpaEntity, UUID> {
}
