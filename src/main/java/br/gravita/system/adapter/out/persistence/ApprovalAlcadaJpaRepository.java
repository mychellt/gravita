package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.ApprovalModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ApprovalAlcadaJpaRepository extends JpaRepository<ApprovalAlcadaJpaEntity, UUID> {
	Optional<ApprovalAlcadaJpaEntity> findByModule(ApprovalModule module);
}
