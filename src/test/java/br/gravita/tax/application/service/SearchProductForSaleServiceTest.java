package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import br.gravita.core.ports.inbound.tax.ProductSearchResult;
import br.gravita.core.ports.inbound.tax.SearchProductQuery;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.usercases.tax.SearchProductForSaleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchProductForSaleServiceTest {

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	private SearchProductForSaleService service;

	@BeforeEach
	void setUp() {
		service = new SearchProductForSaleService(productRepositoryPort, priceTableRepositoryPort, customerRepositoryPort);
	}

	@Test
	@DisplayName("Matches a product by barcode")
	void ac1_matchesByBarcode() {
		ProductDomain product = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		when(productRepositoryPort.findAll()).thenReturn(List.of(product));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("7891234567895", null));

		assertThat(results).extracting(ProductSearchResult::productId).containsExactly(product.getId());
	}

	@Test
	@DisplayName("Matches a product by a substring of the internal code")
	void ac1_matchesByInternalCodeSubstring() {
		ProductDomain product = product("SKU-COLA-2L", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		when(productRepositoryPort.findAll()).thenReturn(List.of(product));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("cola", null));

		assertThat(results).extracting(ProductSearchResult::productId).containsExactly(product.getId());
	}

	@Test
	@DisplayName("Returns an empty list when nothing matches")
	void ac1_noMatchReturnsEmptyList() {
		ProductDomain product = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		when(productRepositoryPort.findAll()).thenReturn(List.of(product));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("does-not-exist", null));

		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("Excludes out-of-stock and inactive products")
	void ac4_excludesOutOfStockAndInactiveProducts() {
		ProductDomain active = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		ProductDomain outOfStock = product("SKU-2", List.of("7891234567896"), ProductStatus.OUT_OF_STOCK, "9.90");
		ProductDomain inactive = product("SKU-3", List.of("7891234567897"), ProductStatus.INACTIVE, "9.90");
		when(productRepositoryPort.findAll()).thenReturn(List.of(active, outOfStock, inactive));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("SKU", null));

		assertThat(results).extracting(ProductSearchResult::productId).containsExactly(active.getId());
	}

	@Test
	@DisplayName("Falls back to the base price when no customer is set")
	void ac3_fallsBackToBasePriceWhenNoCustomerIsSet() {
		ProductDomain product = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		when(productRepositoryPort.findAll()).thenReturn(List.of(product));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("SKU-1", null));

		assertThat(results).extracting(ProductSearchResult::unitPrice)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("9.90"));
	}

	@Test
	@DisplayName("Uses the customer's linked price table when a customer is set")
	void ac3_usesTheCustomersLinkedPriceTableWhenACustomerIsSet() {
		ProductDomain product = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		UUID customerId = UUID.randomUUID();
		PriceTableId priceTableId = PriceTableId.of(UUID.randomUUID());
		PriceTable priceTable = PriceTable.of(priceTableId, PriceFormation.FIXED, LocalDate.now().minusDays(1), null,
				null, null, List.of(new PriceTableEntry(ProductOrClassRef.product(product.getId().toString()), new BigDecimal("7.50"))));
		CustomerDomain customer = CustomerDomain.builder()
				.priceTables(List.of(CustomerPriceTableLink.builder().priceTableId(priceTableId.value()).priority(1).build()))
				.build();

		when(productRepositoryPort.findAll()).thenReturn(List.of(product));
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(customer));
		when(priceTableRepositoryPort.findById(priceTableId)).thenReturn(Optional.of(priceTable));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("SKU-1", customerId));

		assertThat(results).extracting(ProductSearchResult::unitPrice)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("7.50"));
	}

	@Test
	@DisplayName("Falls back to the base price when the customer's price table has no entry for the product")
	void ac3_fallsBackToBasePriceWhenTheCustomersPriceTableHasNoEntryForTheProduct() {
		ProductDomain product = product("SKU-1", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");
		UUID customerId = UUID.randomUUID();
		PriceTableId priceTableId = PriceTableId.of(UUID.randomUUID());
		PriceTable priceTable = PriceTable.of(priceTableId, PriceFormation.FIXED, LocalDate.now().minusDays(1), null,
				null, null, List.of(new PriceTableEntry(ProductOrClassRef.product(UUID.randomUUID().toString()), new BigDecimal("7.50"))));
		CustomerDomain customer = CustomerDomain.builder()
				.priceTables(List.of(CustomerPriceTableLink.builder().priceTableId(priceTableId.value()).priority(1).build()))
				.build();

		when(productRepositoryPort.findAll()).thenReturn(List.of(product));
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(customer));
		when(priceTableRepositoryPort.findById(priceTableId)).thenReturn(Optional.of(priceTable));

		List<ProductSearchResult> results = service.execute(new SearchProductQuery("SKU-1", customerId));

		assertThat(results).extracting(ProductSearchResult::unitPrice)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("9.90"));
	}

	private static ProductDomain product(String internalCode, List<String> barcodes, ProductStatus status, String basePrice) {
		ProductDomain product = ProductDomain.builder()
				.internalCode(internalCode)
				.barcodes(barcodes)
				.type(ProductType.SIMPLE)
				.status(status)
				.basePrice(new BigDecimal(basePrice))
				.build();
		product.setId(UUID.randomUUID());
		return product;
	}
}
