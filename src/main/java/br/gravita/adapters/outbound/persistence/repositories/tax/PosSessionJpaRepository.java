package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.core.domain.tax.PosSessionStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosSessionJpaRepository extends JpaRepository<PosSessionJpaEntity, UUID> {

	boolean existsByRegisterIdAndStatus(UUID registerId, PosSessionStatus status);
}
