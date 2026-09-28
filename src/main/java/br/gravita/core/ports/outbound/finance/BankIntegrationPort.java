package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.math.BigDecimal;
import java.time.Instant;
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

	/**
	 * Creates a dynamic PIX charge at the collection bank.
	 *
	 * @throws br.gravita.core.domain.finance.BankIntegrationUnavailableException
	 *             if the bank isn't configured or can't be reached
	 */
	IssuedPixCharge issuePixCharge(PixChargeIssueRequest request);

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

	/** {@code pixChargeId} is the charge's identity on our side; the bank echoes it back (as the txid) on confirmation. */
	record PixChargeIssueRequest(UUID pixChargeId, UUID receivableId, UUID customerId, BigDecimal amount,
			LocalDate dueDate) {
		public PixChargeIssueRequest {
			Objects.requireNonNull(pixChargeId, "pixChargeId is required");
			Objects.requireNonNull(receivableId, "receivableId is required");
			Objects.requireNonNull(customerId, "customerId is required");
			Objects.requireNonNull(amount, "amount is required");
			Objects.requireNonNull(dueDate, "dueDate is required");
		}
	}

	record IssuedPixCharge(String dynamicQrPayload, Instant expiresAt) {
		public IssuedPixCharge {
			Objects.requireNonNull(dynamicQrPayload, "dynamicQrPayload is required");
			Objects.requireNonNull(expiresAt, "expiresAt is required");
		}
	}
}
