package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FollowUpTaskJpaEntity;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpTaskJpaRepository extends JpaRepository<FollowUpTaskJpaEntity, UUID> {

	boolean existsByOpportunityIdAndDueDate(UUID opportunityId, LocalDate dueDate);

	boolean existsByCustomerIdAndDueDate(UUID customerId, LocalDate dueDate);
}
