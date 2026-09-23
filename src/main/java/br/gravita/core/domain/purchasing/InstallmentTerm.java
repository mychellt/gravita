package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One accounts-payable installment ({@code amount} due on {@code dueDate})
 * carried by a {@link PurchaseReceipt} - either imported from the supplier's
 * NF-e (UC-M6-07) or, when no NF was imported, entered directly at receiving
 * (UC-M6-06) from the order's own agreed terms. UC-M6-08 forwards these as-is
 * to `finance`'s {@code GeneratePayableFromReceiptPort} without recomputing
 * them, so the "NF terms, or the order's terms when no NF was imported"
 * fallback described in docs/specs/m6-compras/uc-08-confirm-purchase-receipt.md
 * is resolved by whichever upstream use case populates the receipt, not here.
 */
public record InstallmentTerm(BigDecimal amount, LocalDate dueDate) {

	public InstallmentTerm {
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(dueDate, "dueDate is required");
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Installment amount must be positive: " + amount);
		}
	}
}
