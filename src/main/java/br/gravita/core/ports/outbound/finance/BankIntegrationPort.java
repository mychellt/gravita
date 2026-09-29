package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

	/**
	 * The bank's CNAB 240/400 return file for the day, if it has published one.
	 *
	 * @throws br.gravita.core.domain.finance.BankIntegrationUnavailableException
	 *             if the bank isn't configured or can't be reached
	 */
	Optional<String> fetchReturnFile(BankIntegration bankIntegration);

	/**
	 * Parses a CNAB 240/400 return payload into its detail lines, in file
	 * order; the layout is the bank's, hence the parameter.
	 *
	 * @throws br.gravita.core.domain.finance.BankIntegrationUnavailableException
	 *             if the bank isn't configured
	 * @throws br.gravita.core.domain.shared.BusinessRuleException
	 *             if the payload isn't a valid return file
	 */
	List<BankReturnLine> parseReturnFile(BankIntegration bankIntegration, String fileContent);

	/**
	 * Generates a single CNAB payment remittance (remessa) covering every item
	 * of {@code request} and sends it to the bank. Each line carries its
	 * payable's id as the title identifier, which the bank echoes back on the
	 * return file.
	 *
	 * @throws br.gravita.core.domain.finance.BankIntegrationUnavailableException
	 *             if the bank isn't configured or can't be reached
	 */
	IssuedRemittance sendRemittance(RemittanceRequest request);

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

	record RemittanceRequest(BankIntegration bankIntegration, List<RemittanceItem> items) {
		public RemittanceRequest {
			Objects.requireNonNull(bankIntegration, "bankIntegration is required");
			Objects.requireNonNull(items, "items is required");
			if (items.isEmpty()) {
				throw new IllegalArgumentException("items must not be empty");
			}
			items = List.copyOf(items);
		}
	}

	/** One payment of a remittance; {@code supplierId} is null for expenses that have no supplier. */
	record RemittanceItem(UUID payableId, UUID supplierId, BigDecimal amount, LocalDate dueDate) {
		public RemittanceItem {
			Objects.requireNonNull(payableId, "payableId is required");
			Objects.requireNonNull(amount, "amount is required");
			Objects.requireNonNull(dueDate, "dueDate is required");
		}
	}

	/** {@code reference} identifies the remittance at the bank; {@code fileContent} is the CNAB payload sent. */
	record IssuedRemittance(String reference, String fileContent) {
		public IssuedRemittance {
			Objects.requireNonNull(reference, "reference is required");
			Objects.requireNonNull(fileContent, "fileContent is required");
		}
	}

	/**
	 * One detail line of a return file. {@code titleIdentifier} is the title
	 * reference the bank echoes back (the receivable's id) and {@code lineNumber}
	 * is the 1-based line in the file. {@code paid} is false for occurrences
	 * that are not a payment (registration confirmation, rejection, ...); a paid
	 * line carries the principal {@code amount} credited against the title, what
	 * was paid on top of it ({@code interest}, {@code fine}, {@code surcharge}),
	 * the {@code discount} granted, and the date the bank received the money.
	 */
	record BankReturnLine(int lineNumber, String titleIdentifier, boolean paid, BigDecimal amount,
			BigDecimal interest, BigDecimal fine, BigDecimal discount, BigDecimal surcharge, LocalDate paidAt) {
		public BankReturnLine {
			Objects.requireNonNull(titleIdentifier, "titleIdentifier is required");
			if (paid) {
				Objects.requireNonNull(amount, "amount is required on a paid line");
				Objects.requireNonNull(paidAt, "paidAt is required on a paid line");
			}
		}
	}
}
