package br.gravita.adapters.outbound.persistence.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** The lookups behind the NF-e item suggestions, against the real database (they use a JPQL string function). */
@SpringBootTest
class ProductRepositoryAdapterLookupIntegrationTest {

	private static final String NCM_DIGITS = "69072200";
	private static final String GTIN = "7908597504167";

	@Autowired
	private ProductRepositoryPort products;
	@Autowired
	private JdbcTemplate jdbc;

	private final List<UUID> savedIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		savedIds.forEach(id -> {
			jdbc.update("delete from product_barcodes where product_id = ?", id);
			jdbc.update("delete from products where id = ?", id);
		});
	}

	private UUID save(final String ncm, final String barcode) {
		final UUID id = UUID.randomUUID();
		products.save(ProductDomain.builder()
				.id(id).internalCode("LOOKUP-" + id).ncm(ncm)
				.barcodes(barcode == null ? List.of() : List.of(barcode))
				.type(ProductType.SIMPLE).status(ProductStatus.ACTIVE)
				.averageCost(BigDecimal.ONE).basePrice(BigDecimal.TEN)
				.build());
		savedIds.add(id);
		return id;
	}

	@Test
	@DisplayName("Finds products by NCM digits whether the NCM was stored with dots or without")
	void findsProductsByNcmDigits() {
		final UUID plain = save(NCM_DIGITS, null);
		final UUID dotted = save("6907.22.00", null);
		final UUID other = save("32091010", null);

		assertThat(products.findAllByNcm(NCM_DIGITS)).extracting(ProductDomain::getId)
				.contains(plain, dotted)
				.doesNotContain(other);
	}

	@Test
	@DisplayName("Finds the product registered under a barcode, with its barcodes loaded")
	void findsAProductByBarcode() {
		final UUID id = save(NCM_DIGITS, GTIN);

		assertThat(products.findByBarcode(GTIN)).get().satisfies(product -> {
			assertThat(product.getId()).isEqualTo(id);
			assertThat(product.getBarcodes()).containsExactly(GTIN);
		});
		assertThat(products.findByBarcode("96385074")).isEmpty();
	}
}
