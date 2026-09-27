package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesInvoiceJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceJpaRepository extends JpaRepository<SalesInvoiceJpaEntity, UUID> {
}
