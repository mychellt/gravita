package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestJpaRepository extends JpaRepository<PurchaseRequestJpaEntity, UUID> {
}
