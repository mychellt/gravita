package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceContingencyQueueJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfceContingencyQueueJpaRepository extends JpaRepository<NfceContingencyQueueJpaEntity, UUID> {
}
