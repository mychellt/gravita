package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentSeriesJpaRepository extends JpaRepository<DocumentSeriesJpaEntity, UUID> {
}
