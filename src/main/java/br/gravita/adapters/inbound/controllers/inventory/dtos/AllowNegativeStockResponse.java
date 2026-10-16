package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.ProductDomain;
import java.util.UUID;

public record AllowNegativeStockResponse(UUID productId, boolean allowNegativeStock) {

	public static AllowNegativeStockResponse from(final ProductDomain product) {
		return new AllowNegativeStockResponse(product.getId(), Boolean.TRUE.equals(product.getAllowNegativeStock()));
	}
}
