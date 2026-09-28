package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @param originDocumentRef id of the NFe / NFCe / NFSe the receivables originate from
 * @param installments     the invoice's payment terms, in installment order
 */
public record GenerateReceivableFromInvoicingCommand(UUID customerId, UUID originDocumentRef,
		List<Installment> installments) {

	public GenerateReceivableFromInvoicingCommand {
		Objects.requireNonNull(customerId, "customerId is required");
		Objects.requireNonNull(originDocumentRef, "originDocumentRef is required");
		Objects.requireNonNull(installments, "installments is required");
		installments = List.copyOf(installments);
	}

	public record Installment(LocalDate dueDate, BigDecimal amount) {

		public Installment {
			Objects.requireNonNull(dueDate, "dueDate is required");
			Objects.requireNonNull(amount, "amount is required");
		}
	}
}
