package br.gravita.tax.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SearchProductForSaleEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Autowired
	private CustomerRepositoryPort customerRepositoryPort;

	@Test
	void ac1_matchesByBarcodeOrInternalCode() throws Exception {
		ProductDomain product = seedProduct("SKU-COLA-2L", List.of("7891234567895"), ProductStatus.ACTIVE, "9.90");

		mockMvc.perform(get("/api/pdv/products/search").param("q", "7891234567895"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(product.getId().toString()));

		mockMvc.perform(get("/api/pdv/products/search").param("q", "cola"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(product.getId().toString()));
	}

	@Test
	void ac4_excludesOutOfStockAndInactiveProducts() throws Exception {
		seedProduct("SEARCH-OOS", List.of("7891234500001"), ProductStatus.OUT_OF_STOCK, "5.00");
		seedProduct("SEARCH-INACTIVE", List.of("7891234500002"), ProductStatus.INACTIVE, "5.00");

		mockMvc.perform(get("/api/pdv/products/search").param("q", "SEARCH-"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void ac3_usesTheCustomersLinkedPriceTableWhenACustomerIdIsGiven() throws Exception {
		ProductDomain product = seedProduct("SKU-PRICED", List.of("7891234500003"), ProductStatus.ACTIVE, "20.00");

		PriceTable priceTable = priceTableRepositoryPort.save(PriceTable.of(PriceTableId.of(UUID.randomUUID()),
				PriceFormation.FIXED, LocalDate.now().minusDays(1), null, null, null,
				List.of(new PriceTableEntry(ProductOrClassRef.product(product.getId().toString()), new BigDecimal("15.00")))));

		CustomerDomain customer = CustomerDomain.builder()
				.id(UUID.randomUUID())
				.name("Cliente Teste")
				.documentDomain(Document.cpf("111.444.777-35"))
				.creditLimit(BigDecimal.ZERO)
				.currentBalance(BigDecimal.ZERO)
				.status(CustomerStatus.REGULAR)
				.priceTables(List.of(CustomerPriceTableLink.builder().priceTableId(priceTable.getId().value()).priority(1).build()))
				.build();
		customerRepositoryPort.save(customer);

		mockMvc.perform(get("/api/pdv/products/search").param("q", "SKU-PRICED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].unitPrice").value(20.00));

		mockMvc.perform(get("/api/pdv/products/search").param("q", "SKU-PRICED").param("customerId", customer.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].unitPrice").value(15.00));
	}

	private ProductDomain seedProduct(String internalCode, List<String> barcodes, ProductStatus status, String basePrice) {
		ProductDomain product = ProductDomain.builder()
				.internalCode(internalCode)
				.barcodes(barcodes)
				.type(ProductType.SIMPLE)
				.status(status)
				.basePrice(new BigDecimal(basePrice))
				.build();
		product.setId(UUID.randomUUID());
		return productRepositoryPort.save(product);
	}
}
