package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.DocumentAttachmentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAttachmentJpaRepository extends JpaRepository<DocumentAttachmentJpaEntity, UUID> {
}
