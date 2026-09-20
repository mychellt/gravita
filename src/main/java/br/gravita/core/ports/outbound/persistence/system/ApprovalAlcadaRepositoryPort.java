package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;

import java.util.Optional;

/**
 * What {@code purchasing}'s {@code ApprovePurchaseOrderUseCase}, {@code sales}'s
 * {@code ApproveSalesOrderUseCase} and {@code finance}'s {@code ApprovePayableUseCase} will call
 * at approval-check time to read the threshold currently configured for their module - so
 * updating a threshold never requires touching their code.
 */
public interface ApprovalAlcadaRepositoryPort {

	ApprovalAlcada save(ApprovalAlcada alcada);

	Optional<ApprovalAlcada> findByModule(ApprovalModule module);
}
