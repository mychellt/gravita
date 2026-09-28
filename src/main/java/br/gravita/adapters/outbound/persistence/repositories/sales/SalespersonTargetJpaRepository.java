package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalespersonTargetJpaRepository extends JpaRepository<SalespersonTargetJpaEntity, UUID> {
	Optional<SalespersonTargetJpaEntity> findBySalespersonIdAndReferenceMonth(UUID salespersonId,
			LocalDate referenceMonth);
}
