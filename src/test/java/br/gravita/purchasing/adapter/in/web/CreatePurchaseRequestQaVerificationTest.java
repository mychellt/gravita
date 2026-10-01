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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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
	@DisplayName("QA scenario 1: a user-originated request with requester and items is created and persisted as OPEN")
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
	@DisplayName("QA scenario 2: a minimum-stock trigger request without a requester is created")
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
	@DisplayName("QA scenario 3: a sales-order demand request without a requester is created")
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
	@DisplayName("QA scenario 4: an empty item list is rejected with 400")
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
	@DisplayName("QA scenario 5: a user-originated request without a requester is rejected with 400")
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
	@DisplayName("QA scenario 6: a system-triggered request that carries a requester is rejected with 400")
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
