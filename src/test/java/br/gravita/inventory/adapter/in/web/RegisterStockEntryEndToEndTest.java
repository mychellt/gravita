package br.gravita.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-72: end-to-end verification of RegisterStockEntryUseCase
 * (POST /api/inventory/movements/entry) through the real HTTP stack - real
 * controller, real use case, real H2-backed repositories.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RegisterStockEntryEndToEndTest {

	@Autowired
	private org.springframework.test.web.servlet.MockMvc mockMvc;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void registersAnEntryAndUpdatesTheBalanceAvailableThroughTheReadEndpoint() throws Exception {
		UUID productId = seedProduct(false, false);
		UUID warehouseId = UUID.randomUUID();

		String body = objectMapper.writeValueAsString(Map.of(
				"productId", productId,
				"warehouseId", warehouseId,
				"quantity", 100,
				"unitCost", 12.50,
				"originReference", "PURCHASE_RECEIPT:1",
				"user", UUID.randomUUID()));

		mockMvc.perform(post("/api/inventory/movements/entry").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("ENTRY"))
				.andExpect(jsonPath("$.quantity").value(100));

		mockMvc.perform(get("/api/inventory/products/" + productId + "/balance").param("warehouseId",
						warehouseId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onHand").value(100))
				.andExpect(jsonPath("$.averageCost").value(12.50));
	}

	@Test
	void rejectsAZeroQuantityEntry() throws Exception {
		UUID productId = seedProduct(false, false);

		String body = objectMapper.writeValueAsString(Map.of(
				"productId", productId,
				"warehouseId", UUID.randomUUID(),
				"quantity", 0,
				"unitCost", 12.50,
				"originReference", "PURCHASE_RECEIPT:1",
				"user", UUID.randomUUID()));

		mockMvc.perform(post("/api/inventory/movements/entry").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void requiresExpiryDateWhenTheProductHasLotControlActive() throws Exception {
		UUID productId = seedProduct(true, false);

		String body = objectMapper.writeValueAsString(Map.of(
				"productId", productId,
				"warehouseId", UUID.randomUUID(),
				"quantity", 10,
				"unitCost", 12.50,
				"lot", Map.of("code", "LOT-1"),
				"originReference", "PURCHASE_RECEIPT:1",
				"user", UUID.randomUUID()));

		mockMvc.perform(post("/api/inventory/movements/entry").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void anUnknownProductReturns404() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of(
				"productId", UUID.randomUUID(),
				"warehouseId", UUID.randomUUID(),
				"quantity", 10,
				"unitCost", 12.50,
				"originReference", "PURCHASE_RECEIPT:1",
				"user", UUID.randomUUID()));

		mockMvc.perform(post("/api/inventory/movements/entry").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
	}

	private UUID seedProduct(boolean lotControl, boolean serialControl) {
		UUID id = UUID.randomUUID();
		productRepositoryPort.save(ProductDomain.builder()
				.id(id)
				.internalCode("SKU-" + id)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.lotControl(lotControl)
				.serialControl(serialControl)
				.build());
		return id;
	}
}
