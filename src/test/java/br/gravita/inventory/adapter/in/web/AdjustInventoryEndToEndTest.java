package br.gravita.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AdjustInventoryEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Test
	void appliesAPositiveAdjustmentAndCarriesTheJustification() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "100");

		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantityDelta": 15,
								  "justification": "Cycle count correction",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("ADJUSTMENT"))
				.andExpect(jsonPath("$.justification").value("Cycle count correction"));

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThat(updated.getOnHand()).isEqualByComparingTo("115");
	}

	@Test
	void appliesANegativeAdjustment() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "100");

		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantityDelta": -15,
								  "justification": "Breakage",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated());

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThat(updated.getOnHand()).isEqualByComparingTo("85");
	}

	@Test
	void rejectsAMissingJustification() throws Exception {
		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantityDelta": 15,
								  "justification": "",
								  "user": "%s"
								}
								""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
				.andExpect(status().isBadRequest());
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(BigDecimal.ZERO)
				.inTransit(BigDecimal.ZERO)
				.averageCost(new BigDecimal("10.00"))
				.build());
	}
}
