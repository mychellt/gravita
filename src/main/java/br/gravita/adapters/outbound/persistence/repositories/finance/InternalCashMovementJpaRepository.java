package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashMovementJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalCashMovementJpaRepository extends JpaRepository<InternalCashMovementJpaEntity, UUID> {

	List<InternalCashMovementJpaEntity> findByCashBoxIdAndTimestampGreaterThanEqualOrderByTimestampAsc(UUID cashBoxId,
			Instant from);
}
