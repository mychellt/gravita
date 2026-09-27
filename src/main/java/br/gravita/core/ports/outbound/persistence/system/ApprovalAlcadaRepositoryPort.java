package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;

import java.util.Optional;

public interface ApprovalAlcadaRepositoryPort {

	ApprovalAlcada save(ApprovalAlcada alcada);

	Optional<ApprovalAlcada> findByModule(ApprovalModule module);
}
