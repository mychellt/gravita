package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeTransmissionQueueJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeTransmissionQueueJpaRepository extends JpaRepository<NfeTransmissionQueueJpaEntity, UUID> {
}
