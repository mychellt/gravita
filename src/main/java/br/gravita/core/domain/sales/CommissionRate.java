package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * The commission rate configured for a (salesperson, product) pair, owned by
 * {@code sales} rather than {@code masterdata} since it's sales policy, not
 * product-catalog data (see the m7-vendas-crm module spec's Notes).
 */
public record CommissionRate(UUID salespersonId, UUID productId, BigDecimal rate) {

	public CommissionRate {
		Objects.requireNonNull(salespersonId, "salespersonId is required");
		Objects.requireNonNull(productId, "productId is required");
		if (rate == null || rate.signum() < 0) {
			throw new BusinessRuleException("Commission rate cannot be negative: " + rate);
		}
	}
}
