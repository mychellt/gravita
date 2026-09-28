package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PixChargeJpaEntity;
import br.gravita.core.domain.finance.PixChargeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PixChargeJpaRepository extends JpaRepository<PixChargeJpaEntity, UUID> {

	List<PixChargeJpaEntity> findByReceivableIdOrderByCreatedAt(UUID receivableId);

	List<PixChargeJpaEntity> findByStatusAndExpiresAtBefore(PixChargeStatus status, Instant instant);
}
