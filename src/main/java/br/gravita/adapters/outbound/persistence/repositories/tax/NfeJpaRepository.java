package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeJpaRepository extends JpaRepository<NfeJpaEntity, UUID> {

	List<NfeJpaEntity> findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
			NfeDocumentStatus status, Instant from, Instant to);
}
