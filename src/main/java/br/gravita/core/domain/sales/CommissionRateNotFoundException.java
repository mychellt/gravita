package br.gravita.core.domain.sales;

import java.util.UUID;

public class CommissionRateNotFoundException extends RuntimeException {

	public CommissionRateNotFoundException(final UUID salespersonId, final UUID productId) {
		super("No commission rate configured for salesperson " + salespersonId + " and product " + productId);
	}
}
