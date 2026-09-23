package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * GRA-59: end-to-end verification of CreatePurchaseOrderUseCase
 * (POST /api/purchasing/orders) through the real HTTP stack - real
 * controller, real use case, real H2-backed repositories.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CreatePurchaseOrderEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void anOrderCanBeCreatedFromAnOpenRequestWithoutAFormalQuotation() throws Exception {
		UUID requestId = createOpenRequest();

		String body = """
				{
				  "requestId": "%s",
				  "supplierId": "%s",
				  "items": [
				    {"productId": "%s", "quantity": 10, "unitPrice": 2.50}
				  ]
				}
				""".formatted(requestId, UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void anOrderCarriesAnOptionalQuotationId() throws Exception {
		UUID requestId = createOpenRequest();
		UUID quotationId = UUID.randomUUID();

		String body = """
				{
				  "requestId": "%s",
				  "quotationId": "%s",
				  "supplierId": "%s",
				  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 9.99}]
				}
				""".formatted(requestId, quotationId, UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void creatingAnOrderFromAnUnknownRequestIsRejectedWith404() throws Exception {
		String body = """
				{
				  "requestId": "%s",
				  "supplierId": "%s",
				  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 1}]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isNotFound());
	}

	@Test
	void anEmptyItemListIsRejectedWith400() throws Exception {
		UUID requestId = createOpenRequest();

		String body = """
				{
				  "requestId": "%s",
				  "supplierId": "%s",
				  "items": []
				}
				""".formatted(requestId, UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void convertingTheSameRequestTwiceIsRejectedWith400OnTheSecondAttempt() throws Exception {
		UUID requestId = createOpenRequest();
		String body = """
				{
				  "requestId": "%s",
				  "supplierId": "%s",
				  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 1}]
				}
				""".formatted(requestId, UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	private UUID createOpenRequest() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "requestedBy": "%s",
				  "items": [{"productId": "%s", "quantity": 10}]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		String response = mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response).get("id").asString());
	}
}
