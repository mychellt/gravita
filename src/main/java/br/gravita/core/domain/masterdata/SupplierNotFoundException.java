package br.gravita.core.domain.masterdata;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {

	public SupplierNotFoundException(final UUID supplierId) {
		super("Supplier not found: " + supplierId);
	}
}
