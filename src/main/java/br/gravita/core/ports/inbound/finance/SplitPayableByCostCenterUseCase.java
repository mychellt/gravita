package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-16: splits (rateia) a payable's amount across one or more cost centers
 * by percentage, for expense reporting and the DRE gerencial. The percentages
 * must add up to 100 and every cost center must exist; the new split replaces
 * any previous one.
 */
public interface SplitPayableByCostCenterUseCase {

	Payable execute(SplitPayableByCostCenterCommand command);
}
