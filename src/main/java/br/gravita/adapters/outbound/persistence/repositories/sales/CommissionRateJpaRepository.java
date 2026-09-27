package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionRateJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRateJpaRepository extends JpaRepository<CommissionRateJpaEntity, UUID> {
	Optional<CommissionRateJpaEntity> findBySalespersonIdAndProductId(UUID salespersonId, UUID productId);
}
