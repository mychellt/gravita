package br.gravita.adapters.outbound.persistence.repositories.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderJpaRepository extends JpaRepository<SalesOrderJpaEntity, UUID> {
}
