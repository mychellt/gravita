package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashClosingReportJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashClosingReportJpaRepository extends JpaRepository<CashClosingReportJpaEntity, UUID> {

	Optional<CashClosingReportJpaEntity> findBySessionId(UUID sessionId);
}
