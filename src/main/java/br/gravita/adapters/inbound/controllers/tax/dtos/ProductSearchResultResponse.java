package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.ProductSearchResult;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchResultResponse(UUID productId, String description, BigDecimal unitPrice,
		boolean availableForSale) {

	public static ProductSearchResultResponse from(ProductSearchResult result) {
		return new ProductSearchResultResponse(result.productId(), result.description(), result.unitPrice(),
				result.availableForSale());
	}
}
