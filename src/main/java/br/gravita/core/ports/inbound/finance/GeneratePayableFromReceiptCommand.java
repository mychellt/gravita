package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @param supplierId         the supplier the goods were bought from
 * @param purchaseReceiptRef id of the confirmed purchase receipt the payables originate from
 * @param installments       the purchase NF's payment terms, in installment order
 */
public record GeneratePayableFromReceiptCommand(UUID supplierId, UUID purchaseReceiptRef,
		List<Installment> installments) {

	public GeneratePayableFromReceiptCommand {
		Objects.requireNonNull(supplierId, "supplierId is required");
		Objects.requireNonNull(purchaseReceiptRef, "purchaseReceiptRef is required");
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
