package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationJpaRepository extends JpaRepository<QuotationJpaEntity, UUID> {
}
