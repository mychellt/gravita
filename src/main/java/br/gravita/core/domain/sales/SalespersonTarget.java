package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public record SalespersonTarget(UUID salespersonId, YearMonth month, BigDecimal valueTarget, long orderCountTarget) {

	public SalespersonTarget {
		Objects.requireNonNull(salespersonId, "salespersonId is required");
		Objects.requireNonNull(month, "month is required");
		if (valueTarget == null || valueTarget.signum() < 0) {
			throw new BusinessRuleException("Value target cannot be negative: " + valueTarget);
		}
		if (orderCountTarget < 0) {
			throw new BusinessRuleException("Order count target cannot be negative: " + orderCountTarget);
		}
	}
}
