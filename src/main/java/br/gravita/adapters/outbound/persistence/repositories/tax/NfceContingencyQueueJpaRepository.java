package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceContingencyQueueJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfceContingencyQueueJpaRepository extends JpaRepository<NfceContingencyQueueJpaEntity, UUID> {

	List<NfceContingencyQueueJpaEntity> findByNextRetryAtLessThanEqual(Instant asOf);

	Optional<NfceContingencyQueueJpaEntity> findByDocumentId(UUID documentId);
}
