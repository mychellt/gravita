package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDomainTest {

	@DisplayName("Rejects a product with more than five images")
	@Test
	void shouldRejectMoreThanFiveImages() {
		final ProductDomain product = simpleProductBuilder()
				.images(List.of("1", "2", "3", "4", "5", "6"))
				.build();

		assertThatThrownBy(product::validate)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("5 images");
	}

	@DisplayName("Accepts a product with exactly five images")
	@Test
	void shouldAcceptExactlyFiveImages() {
		final ProductDomain product = simpleProductBuilder()
				.images(List.of("1", "2", "3", "4", "5"))
				.build();

		assertThatCode(product::validate).doesNotThrowAnyException();
	}

	@DisplayName("Rejects a barcode with an invalid format")
	@Test
	void shouldRejectInvalidBarcodeFormat() {
		final ProductDomain product = simpleProductBuilder()
				.barcodes(List.of("not-a-barcode"))
				.build();

		assertThatThrownBy(product::validate).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Accepts EAN-13 and DUN-14 barcodes")
	@Test
	void shouldAcceptEan13AndDun14Barcodes() {
		final ProductDomain product = simpleProductBuilder()
				.barcodes(List.of("7891234567895", "17891234567892"))
				.build();

		assertThatCode(product::validate).doesNotThrowAnyException();
	}

	@DisplayName("Rejects a service that carries stock fields")
	@Test
	void shouldRejectServiceWithStockFields() {
		final ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.stock(new StockParametersDomain(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE))
				.build();

		assertThatThrownBy(service::validate)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("stock");
	}

	@DisplayName("Rejects a service that carries units of measure")
	@Test
	void shouldRejectServiceWithUnits() {
		final ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.purchaseUnit("UN")
				.build();

		assertThatThrownBy(service::validate).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Accepts a service without stock fields or units")
	@Test
	void shouldAcceptServiceWithoutStockOrUnits() {
		final ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.build();

		assertThatCode(service::validate).doesNotThrowAnyException();
	}

	@DisplayName("A kit requires at least one component")
	@Test
	void shouldRequireAtLeastOneComponentForKit() {
		final ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of())
				.build();

		assertThatThrownBy(kit::validate).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Accepts a kit that has components")
	@Test
	void shouldAcceptKitWithComponents() {
		final ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of(new KitComponentDomain(UUID.randomUUID(), BigDecimal.ONE)))
				.build();

		assertThatCode(kit::validate).doesNotThrowAnyException();
	}

	@DisplayName("A variant product requires a variant grid")
	@Test
	void shouldRequireVariantGridForVariantType() {
		final ProductDomain variant = ProductDomain.builder()
				.type(ProductType.VARIANT)
				.internalCode("VAR-1")
				.variants(List.of())
				.build();

		assertThatThrownBy(variant::validate).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Accepts a variant product with a color and size grid")
	@Test
	void shouldAcceptVariantWithColorSizeGrid() {
		final ProductDomain variant = ProductDomain.builder()
				.type(ProductType.VARIANT)
				.internalCode("VAR-1")
				.variants(List.of(new ProductVariantDomain("Red", "M", null), new ProductVariantDomain("Blue", "G", null)))
				.build();

		assertThatCode(variant::validate).doesNotThrowAnyException();
	}

	private ProductDomain.ProductDomainBuilder<?, ?> simpleProductBuilder() {
		return ProductDomain.builder()
				.type(ProductType.SIMPLE)
				.internalCode("SIMPLE-1");
	}
}
