package br.gravita.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-88: end-to-end verification of SuggestReorderUseCase
 * (GET /api/inventory/alerts/low-stock) through the real HTTP stack - real
 * controller, real use case, real H2-backed repositories. The log-only
 * {@code NotifyLowStockAdapter} runs for real since it has no external
 * dependency (M9 stub).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class LowStockAlertEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Test
	void ac3_returnsSuggestionsForProductsAtOrBelowTheirReorderPoint() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID lowProductId = seedProduct("10", "100", "20");
		seedBalance(lowProductId, warehouseId, "15");
		UUID healthyProductId = seedProduct("10", "100", "20");
		seedBalance(healthyProductId, warehouseId, "50");

		mockMvc.perform(get("/api/inventory/alerts/low-stock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.productId=='" + lowProductId + "')]").exists())
				.andExpect(jsonPath("$[?(@.productId=='" + healthyProductId + "')]").doesNotExist());
	}

	@Test
	void ac3_returnsAnEmptyListWhenNothingIsBelowItsReorderPoint() throws Exception {
		mockMvc.perform(get("/api/inventory/alerts/low-stock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	private UUID seedProduct(String minimum, String maximum, String reorderPoint) {
		UUID id = UUID.randomUUID();
		productRepositoryPort.save(ProductDomain.builder()
				.id(id)
				.internalCode("SKU-" + id)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.stock(new StockParametersDomain(new BigDecimal(minimum), new BigDecimal(maximum),
						new BigDecimal(reorderPoint)))
				.build());
		return id;
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand) {
		stockBalanceRepositoryPort.save(StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal(onHand), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN));
	}
}
