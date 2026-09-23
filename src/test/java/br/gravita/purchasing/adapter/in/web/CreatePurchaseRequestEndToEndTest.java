package br.gravita.purchasing.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
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
 * GRA-56: end-to-end verification of CreatePurchaseRequestUseCase
 * (POST /api/purchasing/requests) through the real HTTP stack - real
 * controller, real use case, real H2-backed repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CreatePurchaseRequestEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void aUserCanManuallyCreateARequestWithAnArbitraryItemList() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "requestedBy": "%s",
				  "items": [
				    {"productId": "%s", "quantity": 10},
				    {"productId": "%s", "quantity": 2.5}
				  ]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void aMinStockTriggerCanCreateARequestWithoutARequester() throws Exception {
		String body = """
				{
				  "origin": "MIN_STOCK_TRIGGER",
				  "items": [{"productId": "%s", "quantity": 5}]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void aSalesOrderDemandCanCreateARequestWithoutARequester() throws Exception {
		String body = """
				{
				  "origin": "SALES_ORDER_DEMAND",
				  "items": [{"productId": "%s", "quantity": 5}]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void anEmptyItemListIsRejectedWith400() throws Exception {
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
	void aUserOriginRequestWithoutARequesterIsRejectedWith400() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "items": [{"productId": "%s", "quantity": 5}]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("requestedBy is required")));
	}
}
