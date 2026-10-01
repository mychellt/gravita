package br.gravita.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetStockBalanceEndToEndTest {

	@Autowired
	private org.springframework.test.web.servlet.MockMvc mockMvc;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Test
	@DisplayName("Returns the on-hand, reserved and available balance for a specific warehouse")
	void returnsTheBalanceForASpecificWarehouse() throws Exception {
		UUID productId = seedProduct();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "50", "20", "0", "10.00");

		mockMvc.perform(get("/api/inventory/products/" + productId + "/balance")
						.param("warehouseId", warehouseId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warehouseId").value(warehouseId.toString()))
				.andExpect(jsonPath("$.onHand").value(50))
				.andExpect(jsonPath("$.reserved").value(20))
				.andExpect(jsonPath("$.available").value(30));
	}

	@Test
	@DisplayName("Omitting the warehouse id aggregates the balance across every warehouse")
	void omittingWarehouseIdAggregatesAcrossEveryWarehouse() throws Exception {
		UUID productId = seedProduct();
		seedBalance(productId, UUID.randomUUID(), "30", "0", "0", "8.00");
		seedBalance(productId, UUID.randomUUID(), "70", "5", "0", "12.00");

		mockMvc.perform(get("/api/inventory/products/" + productId + "/balance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warehouseId").doesNotExist())
				.andExpect(jsonPath("$.onHand").value(100))
				.andExpect(jsonPath("$.available").value(95));
	}

	@Test
	@DisplayName("Querying the balance of an unknown product returns 404")
	void anUnknownProductReturns404() throws Exception {
		mockMvc.perform(get("/api/inventory/products/" + UUID.randomUUID() + "/balance"))
				.andExpect(status().isNotFound());
	}

	private UUID seedProduct() {
		UUID id = UUID.randomUUID();
		productRepositoryPort.save(ProductDomain.builder()
				.id(id)
				.internalCode("SKU-" + id)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.build());
		return id;
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand, String reserved, String inTransit,
			String averageCost) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(new BigDecimal(reserved))
				.inTransit(new BigDecimal(inTransit))
				.averageCost(new BigDecimal(averageCost))
				.build());
	}
}
