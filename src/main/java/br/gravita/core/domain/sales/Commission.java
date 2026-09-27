package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A salesperson/product commission line computed from a single invoiced
 * {@link SalesOrderItem} (UC-M7-08). {@code rate} is copied from the
 * {@link CommissionRate} configured for the pair at calculation time, so a
 * later rate change never retroactively alters an already-computed commission.
 */
public record Commission(CommissionId id, UUID salespersonId, UUID productId, SalesOrderId orderId, BigDecimal rate,
		BigDecimal amount) {

	public Commission {
		Objects.requireNonNull(id, "id is required");
		Objects.requireNonNull(salespersonId, "salespersonId is required");
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(orderId, "orderId is required");
		if (rate == null || rate.signum() < 0) {
			throw new BusinessRuleException("Commission rate cannot be negative: " + rate);
		}
		if (amount == null || amount.signum() < 0) {
			throw new BusinessRuleException("Commission amount cannot be negative: " + amount);
		}
	}

	public static Commission calculate(CommissionId id, UUID salespersonId, UUID productId, SalesOrderId orderId,
			BigDecimal rate, BigDecimal lineTotal) {
		return new Commission(id, salespersonId, productId, orderId, rate, lineTotal.multiply(rate));
	}
}
