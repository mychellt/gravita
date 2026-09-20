package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import java.util.UUID;

import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTableJpaRepository extends JpaRepository<PriceTableJpaEntity, UUID> {
}
