package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record InstallmentTerm(BigDecimal amount, LocalDate dueDate) {

	public InstallmentTerm {
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(dueDate, "dueDate is required");
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Installment amount must be positive: " + amount);
		}
	}
}
