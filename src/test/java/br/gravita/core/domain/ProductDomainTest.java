package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDomainTest {

	@Test
	void shouldRejectMoreThanFiveImages() {
		ProductDomain product = simpleProductBuilder()
				.images(List.of("1", "2", "3", "4", "5", "6"))
				.build();

		assertThatThrownBy(product::validate)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("5 images");
	}

	@Test
	void shouldAcceptExactlyFiveImages() {
		ProductDomain product = simpleProductBuilder()
				.images(List.of("1", "2", "3", "4", "5"))
				.build();

		assertThatCode(product::validate).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectInvalidBarcodeFormat() {
		ProductDomain product = simpleProductBuilder()
				.barcodes(List.of("not-a-barcode"))
				.build();

		assertThatThrownBy(product::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptEan13AndDun14Barcodes() {
		ProductDomain product = simpleProductBuilder()
				.barcodes(List.of("7891234567895", "17891234567892"))
				.build();

		assertThatCode(product::validate).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectServiceWithStockFields() {
		ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.stock(new StockParametersDomain(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE))
				.build();

		assertThatThrownBy(service::validate)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("stock");
	}

	@Test
	void shouldRejectServiceWithUnits() {
		ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.purchaseUnit("UN")
				.build();

		assertThatThrownBy(service::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptServiceWithoutStockOrUnits() {
		ProductDomain service = ProductDomain.builder()
				.type(ProductType.SERVICE)
				.internalCode("SVC-1")
				.build();

		assertThatCode(service::validate).doesNotThrowAnyException();
	}

	@Test
	void shouldRequireAtLeastOneComponentForKit() {
		ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of())
				.build();

		assertThatThrownBy(kit::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptKitWithComponents() {
		ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of(new KitComponentDomain(UUID.randomUUID(), BigDecimal.ONE)))
				.build();

		assertThatCode(kit::validate).doesNotThrowAnyException();
	}

	@Test
	void shouldRequireVariantGridForVariantType() {
		ProductDomain variant = ProductDomain.builder()
				.type(ProductType.VARIANT)
				.internalCode("VAR-1")
				.variants(List.of())
				.build();

		assertThatThrownBy(variant::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptVariantWithColorSizeGrid() {
		ProductDomain variant = ProductDomain.builder()
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
