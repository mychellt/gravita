package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.PixCharge;

/**
 * UC-M8-04: creates a dynamic PIX charge ({@code PENDING}) carrying the amount
 * and due date of an {@code OPEN} {@code Receivable} through the bank
 * integration. A receivable that isn't {@code OPEN} is rejected before the
 * bank is contacted.
 */
public interface GeneratePixChargeUseCase {

	PixCharge execute(GeneratePixChargeCommand command);
}
