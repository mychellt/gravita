package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierProductCatalogLinkResponse(UUID id, UUID supplierId, String supplierProductCode, UUID productId,
		BigDecimal lastPurchaseUnitValue, Instant lastPurchaseAt) {

	public static SupplierProductCatalogLinkResponse from(final SupplierProductCatalogLink link) {
		return new SupplierProductCatalogLinkResponse(link.getId().value(), link.getSupplierId().value(),
				link.getSupplierProductCode(), link.getProductId(), link.getLastPurchaseUnitValue(), link.getLastPurchaseAt());
	}
}
