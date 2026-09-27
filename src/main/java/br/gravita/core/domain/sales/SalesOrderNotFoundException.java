package br.gravita.core.domain.sales;

import java.util.UUID;

public class SalesOrderNotFoundException extends RuntimeException {

	public SalesOrderNotFoundException(UUID orderId) {
		super("Sales order not found: " + orderId);
	}
}
