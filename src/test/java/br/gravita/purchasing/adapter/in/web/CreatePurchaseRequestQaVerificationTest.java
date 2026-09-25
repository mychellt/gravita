package br.gravita.purchasing.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * GRA-65: independent QA verification of GRA-56's CreatePurchaseRequestUseCase
 * (POST /api/purchasing/requests), written fresh against the real HTTP stack
 * rather than reusing the dev-authored CreatePurchaseRequestEndToEndTest. Covers
 * every scenario in the GRA-65 reproducible test plan, including reading the
 * persisted row back through PurchaseRequestRepositoryPort to confirm status is
 * always OPEN (item 7 of the plan - no GET endpoint exists yet).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CreatePurchaseRequestQaVerificationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	@Test
	void scenario1_userOriginWithRequestedByAndItemsIsCreatedAndPersistedAsOpen() throws Exception {
		UUID requestedBy = UUID.randomUUID();
		String body = """
				{
				  "origin": "USER",
				  "requestedBy": "%s",
				  "items": [{"productId": "%s", "quantity": 3}]
				}
				""".formatted(requestedBy, UUID.randomUUID());

		UUID id = createAndExtractId(body);

		assertPersistedStatusIsOpen(id);
	}

	@Test
	void scenario2_minStockTriggerOriginWithoutRequestedByIsCreated() throws Exception {
		String body = """
				{
				  "origin": "MIN_STOCK_TRIGGER",
				  "items": [{"productId": "%s", "quantity": 8}]
				}
				""".formatted(UUID.randomUUID());

		UUID id = createAndExtractId(body);

		assertPersistedStatusIsOpen(id);
	}

	@Test
	void scenario3_salesOrderDemandOriginWithoutRequestedByIsCreated() throws Exception {
		String body = """
				{
				  "origin": "SALES_ORDER_DEMAND",
				  "items": [{"productId": "%s", "quantity": 1}]
				}
				""".formatted(UUID.randomUUID());

		UUID id = createAndExtractId(body);

		assertPersistedStatusIsOpen(id);
	}

	@Test
	void scenario4_emptyItemListIsRejectedWith400() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "requestedBy": "%s",
				  "items": []
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void scenario5_userOriginWithoutRequestedByIsRejectedWith400() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "items": [{"productId": "%s", "quantity": 4}]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("requestedBy is required")));
	}

	@Test
	void scenario6_systemTriggeredOriginWithRequestedByPresentIsRejectedWith400() throws Exception {
		String body = """
				{
				  "origin": "MIN_STOCK_TRIGGER",
				  "requestedBy": "%s",
				  "items": [{"productId": "%s", "quantity": 4}]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	private UUID createAndExtractId(String requestBody) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn();
		String responseBody = result.getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(responseBody).get("id").asString());
	}

	private void assertPersistedStatusIsOpen(UUID id) {
		PurchaseRequest persisted = purchaseRequestRepositoryPort.findById(PurchaseRequestId.of(id)).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PurchaseRequestStatus.OPEN);
	}
}
