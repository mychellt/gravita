package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.adapters.ProductRepositoryAdapter;
import br.gravita.core.domain.ClassificationDomain;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.ProductVariantDomain;
import br.gravita.core.domain.StockParametersDomain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ProductRepositoryAdapter.class)
class ProductRepositoryAdapterTest {

	@Autowired
	private ProductRepositoryAdapter repositoryAdapter;

	@Test
	void shouldSaveAndRetrieveSimpleProduct() {
		ProductDomain product = ProductDomain.builder()
				.internalCode("SKU-1")
				.barcodes(List.of("7891234567895"))
				.type(ProductType.SIMPLE)
				.averageCost(new BigDecimal("10.00"))
				.basePrice(new BigDecimal("19.90"))
				.stock(new StockParametersDomain(BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("2")))
				.purchaseUnit("CX")
				.saleUnit("UN")
				.conversionFactor(BigDecimal.TEN)
				.classification(new ClassificationDomain("Bebidas", "Refrigerantes", "Acme", "Mercearia"))
				.images(List.of("http://example.com/1.png"))
				.status(ProductStatus.ACTIVE)
				.build();
		product.setId(UUID.randomUUID());

		ProductDomain saved = repositoryAdapter.save(product);

		assertThat(repositoryAdapter.get(saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getInternalCode()).isEqualTo("SKU-1");
					assertThat(found.getBarcodes()).containsExactly("7891234567895");
					assertThat(found.getStatus()).isEqualTo(ProductStatus.ACTIVE);
					assertThat(found.getClassification().brand()).isEqualTo("Acme");
					assertThat(found.getStock().minimum()).isEqualByComparingTo(BigDecimal.ONE);
				});
	}

	@Test
	void shouldDetectExistingBarcode() {
		ProductDomain product = ProductDomain.builder()
				.internalCode("SKU-2")
				.barcodes(List.of("7891234567895"))
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.build();
		product.setId(UUID.randomUUID());
		repositoryAdapter.save(product);

		assertThat(repositoryAdapter.existsByBarcode("7891234567895")).isTrue();
		assertThat(repositoryAdapter.existsByBarcode("0000000000000")).isFalse();
	}

	@Test
	void shouldSaveKitWithComponentsAndVariantsWithGrid() {
		UUID componentId = UUID.randomUUID();
		ProductDomain kit = ProductDomain.builder()
				.internalCode("KIT-1")
				.type(ProductType.KIT)
				.status(ProductStatus.ACTIVE)
				.kitComponents(List.of(new KitComponentDomain(componentId, BigDecimal.TWO)))
				.build();
		kit.setId(UUID.randomUUID());

		ProductDomain variant = ProductDomain.builder()
				.internalCode("VAR-1")
				.type(ProductType.VARIANT)
				.status(ProductStatus.ACTIVE)
				.variants(List.of(new ProductVariantDomain("Red", "M", null), new ProductVariantDomain("Blue", "G", null)))
				.build();
		variant.setId(UUID.randomUUID());

		ProductDomain savedKit = repositoryAdapter.save(kit);
		ProductDomain savedVariant = repositoryAdapter.save(variant);

		assertThat(repositoryAdapter.get(savedKit.getId())).get()
				.satisfies(found -> assertThat(found.getKitComponents())
						.containsExactly(new KitComponentDomain(componentId, BigDecimal.TWO)));
		assertThat(repositoryAdapter.get(savedVariant.getId())).get()
				.satisfies(found -> assertThat(found.getVariants()).hasSize(2));
	}
}
