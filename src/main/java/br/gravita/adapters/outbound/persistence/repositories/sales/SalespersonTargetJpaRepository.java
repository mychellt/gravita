package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalespersonTargetJpaRepository extends JpaRepository<SalespersonTargetJpaEntity, UUID> {
	Optional<SalespersonTargetJpaEntity> findBySalespersonIdAndMonth(UUID salespersonId, String month);

	List<SalespersonTargetJpaEntity> findByMonth(String month);
}
