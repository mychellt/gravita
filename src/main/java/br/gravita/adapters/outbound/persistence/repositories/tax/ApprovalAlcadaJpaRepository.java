package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.core.domain.system.ApprovalModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ApprovalAlcadaJpaRepository extends JpaRepository<ApprovalAlcadaJpaEntity, UUID> {
	Optional<ApprovalAlcadaJpaEntity> findByModule(ApprovalModule module);
}
