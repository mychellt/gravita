package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import java.util.UUID;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {
}
