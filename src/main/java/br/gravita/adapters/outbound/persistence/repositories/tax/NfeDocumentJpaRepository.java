package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeDocumentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeDocumentJpaRepository extends JpaRepository<NfeDocumentJpaEntity, UUID> {
}
