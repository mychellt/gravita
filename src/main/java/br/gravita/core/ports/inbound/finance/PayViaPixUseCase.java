package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-14: pays a single {@code APPROVED} payable right away with a PIX
 * transfer through the bank integration. On success the payable moves to
 * {@code PAID} and the bank's receipt is stored and attached to it; if the
 * transfer fails the payable stays {@code APPROVED}.
 */
public interface PayViaPixUseCase {

	Payable execute(PayViaPixCommand command);
}
