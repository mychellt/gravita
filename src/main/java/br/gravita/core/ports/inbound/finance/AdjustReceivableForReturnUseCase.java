package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Receivable;

/**
 * UC-M8-21: reduces the open amount of the receivable linked to a sales return
 * (UC-M7-07), or cancels it when the return covers the whole open balance.
 * Only the unsettled portion is adjusted; settled amounts are never clawed back.
 */
public interface AdjustReceivableForReturnUseCase {

	Receivable execute(AdjustReceivableForReturnCommand command);
}
