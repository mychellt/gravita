package br.gravita.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockReservationJpaRepository;
import br.gravita.core.domain.inventory.StockReservationStatus;
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
class RegisterStockExitEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Autowired
	private StockReservationJpaRepository stockReservationJpaRepository;

	@Test
	void registersAnExitAndDecreasesOnHand() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "100", "0");

		mockMvc.perform(post("/api/inventory/movements/exit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 30,
								  "originReference": "SALE:1",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("EXIT"))
				.andExpect(jsonPath("$.quantity").value(30));

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThat(updated.getOnHand()).isEqualByComparingTo("70");
	}

	@Test
	void blocksTheExitWhenAvailableIsInsufficientAndNegativeStockIsNotAllowed() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "10", "0");

		mockMvc.perform(post("/api/inventory/movements/exit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 50,
								  "originReference": "SALE:1",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, UUID.randomUUID())))
				.andExpect(status().isConflict());
	}

	@Test
	void allowsGoingNegativeWhenExplicitlyPermitted() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "10", "0");

		mockMvc.perform(post("/api/inventory/movements/exit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 50,
								  "allowNegativeStock": true,
								  "originReference": "SALE:1",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated());

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThat(updated.getOnHand()).isEqualByComparingTo("-40");
	}

	@Test
	void fulfillingAReservationConsumesItAndUpdatesTheBalance() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "100", "30");
		UUID reservationId = UUID.randomUUID();
		stockReservationJpaRepository.save(StockReservationJpaEntity.builder()
				.id(reservationId)
				.orderRef(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.quantity(new BigDecimal("30"))
				.status(StockReservationStatus.ACTIVE)
				.build());

		mockMvc.perform(post("/api/inventory/movements/exit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "warehouseId": "%s",
								  "quantity": 30,
								  "reservationId": "%s",
								  "originReference": "SALE:1",
								  "user": "%s"
								}
								""".formatted(productId, warehouseId, reservationId, UUID.randomUUID())))
				.andExpect(status().isCreated());

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		assertThat(updated.getOnHand()).isEqualByComparingTo("70");
		assertThat(updated.getReserved()).isEqualByComparingTo("0");
		assertThat(stockReservationJpaRepository.findById(reservationId).orElseThrow().getStatus())
				.isEqualTo(StockReservationStatus.CONSUMED);
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand, String reserved) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(new BigDecimal(reserved))
				.inTransit(BigDecimal.ZERO)
				.averageCost(new BigDecimal("10.00"))
				.build());
	}
}
