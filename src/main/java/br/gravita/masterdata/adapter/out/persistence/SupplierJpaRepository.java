package br.gravita.masterdata.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {
}
