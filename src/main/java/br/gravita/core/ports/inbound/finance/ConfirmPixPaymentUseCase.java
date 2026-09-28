package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.PixCharge;

/**
 * Asynchronous half of UC-M8-04, invoked by the bank integration when a PIX
 * charge is paid: moves the {@code PixCharge} to {@code PAID} and settles the
 * linked {@code Receivable}. Idempotent - a repeated confirmation of a charge
 * that is already {@code PAID} changes nothing.
 */
public interface ConfirmPixPaymentUseCase {

	PixCharge execute(ConfirmPixPaymentCommand command);
}
