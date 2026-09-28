package br.gravita.adapters.outbound.integration.finance;

import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import org.springframework.stereotype.Component;

/**
 * Placeholder until the per-bank API clients (Itaú, BB, Bradesco, Sicoob,
 * Sicredi) exist: no bank is configured yet, so every request fails cleanly
 * with {@link BankIntegrationUnavailableException} rather than fabricating a
 * barcode line.
 */
@Component
class BankIntegrationAdapter implements BankIntegrationPort {

	@Override
	public IssuedBoleto issueBoleto(BoletoIssueRequest request) {
		throw new BankIntegrationUnavailableException(
				"Bank integration not configured: " + request.bankIntegration());
	}
}
