package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChartOfAccountsJpaRepository extends JpaRepository<ChartOfAccountsJpaEntity, UUID> {
    boolean existsByParentId(UUID parentId);
}
