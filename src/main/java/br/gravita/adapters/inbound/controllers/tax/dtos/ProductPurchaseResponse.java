package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.ProductPurchase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPurchaseResponse(UUID inboundNfeId, String series, String number, String supplierDocument,
		String supplierName, Instant issuedAt, BigDecimal quantity, BigDecimal unitValue) {

	public static ProductPurchaseResponse from(final ProductPurchase purchase) {
		return new ProductPurchaseResponse(purchase.inboundNfeId(), purchase.series(), purchase.number(),
				purchase.supplierDocument(), purchase.supplierName(), purchase.issuedAt(), purchase.quantity(),
				purchase.unitValue());
	}
}
