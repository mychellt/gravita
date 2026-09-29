package br.gravita.adapters.outbound.integration.finance;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Placeholder until the per-bank API clients (Itaú, BB, Bradesco, Sicoob,
 * Sicredi) exist: no bank is configured yet, so every request fails cleanly
 * with {@link BankIntegrationUnavailableException} rather than fabricating a
 * barcode line (or parsing a return file).
 */
@Component
class BankIntegrationAdapter implements BankIntegrationPort {

	@Override
	public IssuedBoleto issueBoleto(BoletoIssueRequest request) {
		throw new BankIntegrationUnavailableException(
				"Bank integration not configured: " + request.bankIntegration());
	}

	@Override
	public IssuedPixCharge issuePixCharge(PixChargeIssueRequest request) {
		throw new BankIntegrationUnavailableException("Bank integration not configured for PIX charges");
	}

	@Override
	public PixPaymentReceipt payViaPix(PixPaymentRequest request) {
		throw new BankIntegrationUnavailableException("Bank integration not configured for PIX payments");
	}

	@Override
	public IssuedRemittance sendRemittance(RemittanceRequest request) {
		throw new BankIntegrationUnavailableException(
				"Bank integration not configured: " + request.bankIntegration());
	}

	@Override
	public Optional<String> fetchReturnFile(BankIntegration bankIntegration) {
		throw new BankIntegrationUnavailableException("Bank integration not configured: " + bankIntegration);
	}

	@Override
	public Optional<String> fetchPaymentReturnFile(BankIntegration bankIntegration) {
		throw new BankIntegrationUnavailableException("Bank integration not configured: " + bankIntegration);
	}

	@Override
	public List<BankReturnLine> parseReturnFile(BankIntegration bankIntegration, String fileContent) {
		throw new BankIntegrationUnavailableException("Bank integration not configured: " + bankIntegration);
	}
}
