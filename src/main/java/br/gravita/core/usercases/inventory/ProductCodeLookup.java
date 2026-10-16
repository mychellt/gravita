package br.gravita.core.usercases.inventory;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Resolves the internal code of many products with a single read, for the list use cases to show next to ids. */
final class ProductCodeLookup {

	private ProductCodeLookup() {
	}

	static Map<UUID, String> codesOf(final ProductRepositoryPort productRepositoryPort, final Collection<UUID> productIds) {
		final Map<UUID, String> codes = new HashMap<>();
		productsOf(productRepositoryPort, productIds).forEach((id, product) -> codes.put(id, product.getInternalCode()));
		return codes;
	}

	/** Same single-read lookup as {@link #codesOf}, but keeps the full product for callers that need more than the code. */
	static Map<UUID, ProductDomain> productsOf(final ProductRepositoryPort productRepositoryPort, final Collection<UUID> productIds) {
		final List<UUID> distinctIds = productIds.stream().distinct().toList();
		if (distinctIds.isEmpty()) {
			return Map.of();
		}
		final Map<UUID, ProductDomain> products = new HashMap<>();
		for (final ProductDomain product : productRepositoryPort.findAllByIds(distinctIds)) {
			products.put(product.getId(), product);
		}
		return products;
	}
}
