package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashMovementJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashMovementJpaRepository extends JpaRepository<CashMovementJpaEntity, UUID> {
}
