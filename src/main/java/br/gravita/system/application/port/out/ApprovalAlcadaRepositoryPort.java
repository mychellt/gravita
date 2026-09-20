package br.gravita.system.application.port.out;

import br.gravita.system.domain.model.ApprovalAlcada;
import br.gravita.system.domain.model.ApprovalModule;

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
