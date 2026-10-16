package br.gravita.core.ports.inbound.masterdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Creates the link on first purchase, or just refreshes its last-purchase price/date on a repeat one - the
 * {@code productId} of an existing link is never changed by an upsert (BR8: resolution never rewrites who a code
 * points to once linked; see UC-M1-16 for un-linking, which is out of scope today).
 */
public record UpsertSupplierProductCatalogLinkCommand(
		UUID companyId,
		UUID supplierId,
		String supplierProductCode,
		UUID productId,
		BigDecimal unitValue,
		Instant purchasedAt) {
}
