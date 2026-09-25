package br.gravita.adapters.outbound.persistence.repositories.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockTransferJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferJpaRepository extends JpaRepository<StockTransferJpaEntity, UUID> {
}
