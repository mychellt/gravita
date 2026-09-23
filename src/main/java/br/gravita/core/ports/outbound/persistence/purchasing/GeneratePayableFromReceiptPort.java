package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the finance context (M8), which owns accounts payable.
 * Purchasing has no dependency on M8, so until it exists this is backed by a
 * stub adapter (mirrors {@code FinanceUsageQueryPort}'s pattern); M8's own
 * payable-generation use case (GRA-14) is expected to supply the real adapter
 * once it lands. {@code installments} is passed through as resolved by the
 * receipt (NF terms, or the order's terms when no NF was imported) - this
 * port does not itself decide the fallback.
 */
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
