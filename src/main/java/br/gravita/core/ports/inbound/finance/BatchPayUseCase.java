package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CnabRemittance;

/**
 * UC-M8-13: generates a single CNAB remittance covering several {@code APPROVED}
 * payables and sends it to the bank. The payables are not touched: they move to
 * {@code PAID} only when the bank's return for this remittance is processed
 * (UC-M8-22).
 */
public interface BatchPayUseCase {

	CnabRemittance execute(BatchPayCommand command);
}
