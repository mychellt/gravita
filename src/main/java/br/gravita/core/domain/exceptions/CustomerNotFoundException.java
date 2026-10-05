package br.gravita.core.domain.exceptions;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(final UUID customerId) {
		super("Customer not found: " + customerId);
	}
}
