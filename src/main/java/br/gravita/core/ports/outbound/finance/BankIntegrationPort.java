package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Gateway to the banks' APIs (Itaú, BB, Bradesco, Sicoob, Sicredi); one
 * implementation per bank behind this port. Only boleto issuance is needed so
 * far (UC-M8-03); PIX and CNAB operations join it with their use cases.
 */
public interface BankIntegrationPort {

	/**
	 * @throws br.gravita.core.domain.finance.BankIntegrationUnavailableException
	 *             if the bank isn't configured or can't be reached
	 */
	IssuedBoleto issueBoleto(BoletoIssueRequest request);

	record BoletoIssueRequest(BankIntegration bankIntegration, UUID receivableId, UUID customerId, BigDecimal amount,
			LocalDate dueDate) {
		public BoletoIssueRequest {
			Objects.requireNonNull(bankIntegration, "bankIntegration is required");
			Objects.requireNonNull(receivableId, "receivableId is required");
			Objects.requireNonNull(customerId, "customerId is required");
			Objects.requireNonNull(amount, "amount is required");
			Objects.requireNonNull(dueDate, "dueDate is required");
		}
	}

	record IssuedBoleto(String barcodeLine) {
		public IssuedBoleto {
			Objects.requireNonNull(barcodeLine, "barcodeLine is required");
		}
	}
}
