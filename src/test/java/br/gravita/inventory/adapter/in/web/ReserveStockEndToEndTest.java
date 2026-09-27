package br.gravita.inventory.adapter.in.web;

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
class ReserveStockEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Test
	void reservesStockAndPersistsTheUpdatedBalance() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		UUID orderRef = UUID.randomUUID();
		seedBalance(productId, warehouseId, "50", "10", "0", "9.00");

		mockMvc.perform(post("/api/inventory/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "orderRef": "%s",
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 20
								}
								""".formatted(orderRef, productId, warehouseId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderRef").value(orderRef.toString()))
				.andExpect(jsonPath("$.quantity").value(20))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThatBalanceReflectsTheReservation(updated);
	}

	@Test
	void rejectsAReservationLargerThanAvailableStock() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "10", "0", "0", "9.00");

		mockMvc.perform(post("/api/inventory/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "orderRef": "%s",
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 50
								}
								""".formatted(UUID.randomUUID(), productId, warehouseId)))
				.andExpect(status().isConflict());
	}

	private void assertThatBalanceReflectsTheReservation(StockBalanceJpaEntity updated) {
		org.assertj.core.api.Assertions.assertThat(updated.getOnHand()).isEqualByComparingTo("50");
		org.assertj.core.api.Assertions.assertThat(updated.getReserved()).isEqualByComparingTo("30");
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
