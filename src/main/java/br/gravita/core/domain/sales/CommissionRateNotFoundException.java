package br.gravita.core.domain.sales;

import java.util.UUID;

public class CommissionRateNotFoundException extends RuntimeException {

	public CommissionRateNotFoundException(UUID salespersonId, UUID productId) {
		super("No commission rate configured for salesperson " + salespersonId + " and product " + productId);
	}
}
