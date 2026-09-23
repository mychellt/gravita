package br.gravita.core.domain.purchasing;

import java.util.UUID;

public class QuotationNotFoundException extends RuntimeException {

	public QuotationNotFoundException(UUID quotationId) {
		super("Quotation not found: " + quotationId);
	}
}
