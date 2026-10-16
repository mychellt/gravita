package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression: with open-in-view off, reading a product failed with a LazyInitializationException (HTTP 500 on the
 * inventory endpoints that load products), because the adapter mapped the entity to the domain, touching the lazy
 * {@code barcodes} collection, after its transaction had ended. Runs against the real database.
 */
@SpringBootTest
class ProductRepositoryAdapterLazyLoadingIntegrationTest {

	private static final String BARCODE = "7891234567895";

	@Autowired
	private ProductRepositoryPort products;
	@Autowired
	private JdbcTemplate jdbc;

	private UUID productId;

	@AfterEach
	void cleanUp() {
		if (productId != null) {
			jdbc.update("delete from product_barcodes where product_id = ?", productId);
			jdbc.update("delete from products where id = ?", productId);
		}
	}

	private void saveProductWithBarcode() {
		productId = UUID.randomUUID();
		products.save(ProductDomain.builder()
				.id(productId).internalCode("LAZY-" + productId)
				.barcodes(List.of(BARCODE))
				.type(ProductType.SIMPLE).status(ProductStatus.ACTIVE)
				.averageCost(BigDecimal.ONE).basePrice(BigDecimal.TEN)
				.build());
	}

	@Test
	@DisplayName("Reads a product's barcodes by id, from the full list and by ids without a lazy-loading failure")
	void readsLazyCollections() {
		saveProductWithBarcode();

		assertThat(products.get(productId).orElseThrow().getBarcodes()).containsExactly(BARCODE);
		assertThat(products.findAll())
				.anyMatch(p -> productId.equals(p.getId()) && p.getBarcodes().contains(BARCODE));
		assertThat(products.findAllByIds(List.of(productId)))
				.singleElement().satisfies(p -> assertThat(p.getBarcodes()).containsExactly(BARCODE));
	}
}
