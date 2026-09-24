package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.QuotationId;

/**
 * UC-M6-02: sends an OPEN {@code PurchaseRequest}'s item list to one or more
 * suppliers for pricing, creating a {@code Quotation} and moving the request
 * to {@code QUOTED}. Suppliers' replies are recorded separately (UC-M6-03).
 */
public interface SendQuotationUseCase {
	QuotationId execute(SendQuotationCommand command);
}
