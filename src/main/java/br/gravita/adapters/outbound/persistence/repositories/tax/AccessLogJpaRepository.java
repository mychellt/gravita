package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessLogJpaRepository extends JpaRepository<AccessLogJpaEntity, UUID> {
}
