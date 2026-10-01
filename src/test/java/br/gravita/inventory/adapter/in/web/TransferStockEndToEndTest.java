package br.gravita.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
class TransferStockEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Test
	@DisplayName("Initiating a transfer moves the quantity from on-hand to in-transit at the source warehouse")
	void initiatingMovesOnHandIntoInTransitAtTheSource() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID sourceWarehouseId = UUID.randomUUID();
		UUID destinationWarehouseId = UUID.randomUUID();
		seedBalance(productId, sourceWarehouseId, "100");

		mockMvc.perform(post("/api/inventory/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "sourceWarehouseId": "%s",
								  "destinationWarehouseId": "%s",
								  "quantity": 40,
								  "user": "%s"
								}
								""".formatted(productId, sourceWarehouseId, destinationWarehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("TRANSFER"));

		StockBalanceJpaEntity source = stockBalanceJpaRepository
				.findByProductIdAndWarehouseId(productId, sourceWarehouseId).orElseThrow();
		assertThat(source.getOnHand()).isEqualByComparingTo("60");
		assertThat(source.getInTransit()).isEqualByComparingTo("40");
	}

	@Test
	@DisplayName("Confirming a transfer adds the quantity to the destination warehouse on-hand stock")
	void confirmingMovesTheQuantityIntoTheDestinationOnHand() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID sourceWarehouseId = UUID.randomUUID();
		UUID destinationWarehouseId = UUID.randomUUID();
		seedBalance(productId, sourceWarehouseId, "100");

		String initiateResponse = mockMvc.perform(post("/api/inventory/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": "%s",
								  "sourceWarehouseId": "%s",
								  "destinationWarehouseId": "%s",
								  "quantity": 40,
								  "user": "%s"
								}
								""".formatted(productId, sourceWarehouseId, destinationWarehouseId, UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String transferMovementId = JsonPath.read(initiateResponse, "$.id");

		mockMvc.perform(post("/api/inventory/transfers/" + transferMovementId + "/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "user": "%s" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.warehouseId").value(destinationWarehouseId.toString()));

		StockBalanceJpaEntity source = stockBalanceJpaRepository
				.findByProductIdAndWarehouseId(productId, sourceWarehouseId).orElseThrow();
		StockBalanceJpaEntity destination = stockBalanceJpaRepository
				.findByProductIdAndWarehouseId(productId, destinationWarehouseId).orElseThrow();
		assertThat(source.getInTransit()).isEqualByComparingTo("0");
		assertThat(destination.getOnHand()).isEqualByComparingTo("40");
	}

	@Test
	@DisplayName("Confirming a transfer that does not exist returns 404")
	void confirmingAnUnknownTransferReturns404() throws Exception {
		mockMvc.perform(post("/api/inventory/transfers/" + UUID.randomUUID() + "/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "user": "%s" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound());
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
