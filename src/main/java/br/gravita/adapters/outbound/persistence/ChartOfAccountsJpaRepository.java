package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ChartOfAccountsJpaRepository extends JpaRepository<ChartOfAccountsJpaEntity, UUID> {
	boolean existsByParentId(UUID parentId);
}
