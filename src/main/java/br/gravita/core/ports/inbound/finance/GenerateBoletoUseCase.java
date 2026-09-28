package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Boleto;

/**
 * UC-M8-03: issues a {@code Boleto} for an {@code OPEN} {@code Receivable}
 * through the chosen bank integration, links it to the receivable and
 * e-mails it to the customer. A receivable that isn't {@code OPEN} is
 * rejected before the bank is contacted.
 */
public interface GenerateBoletoUseCase {

	Boleto execute(GenerateBoletoCommand command);
}
