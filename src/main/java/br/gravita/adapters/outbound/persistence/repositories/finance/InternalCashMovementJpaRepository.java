package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashMovementJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalCashMovementJpaRepository extends JpaRepository<InternalCashMovementJpaEntity, UUID> {
}
