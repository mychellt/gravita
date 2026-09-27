package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface GeneratePayableFromReceiptPort {
	void generatePayables(GeneratePayableFromReceiptCommand command);

	record GeneratePayableFromReceiptCommand(UUID sourcePurchaseReceiptId, UUID supplierId,
			List<Installment> installments) {

		public GeneratePayableFromReceiptCommand {
			Objects.requireNonNull(sourcePurchaseReceiptId, "sourcePurchaseReceiptId is required");
			Objects.requireNonNull(supplierId, "supplierId is required");
			installments = installments == null ? List.of() : List.copyOf(installments);
		}

		public record Installment(BigDecimal amount, LocalDate dueDate) {
			public Installment {
				Objects.requireNonNull(amount, "amount is required");
				Objects.requireNonNull(dueDate, "dueDate is required");
			}
		}
	}
}
