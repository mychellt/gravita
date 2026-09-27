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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SendQuotationEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void aBuyerCanSendAQuotationForAnOpenRequestToMultipleSuppliers() throws Exception {
		UUID requestId = createOpenPurchaseRequest();

		String body = """
				{
				  "suppliers": ["%s", "%s"]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests/{id}/quotations", requestId)
						.contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void sendingAQuotationTwiceForTheSameRequestIsRejectedWith400() throws Exception {
		UUID requestId = createOpenPurchaseRequest();
		String body = """
				{
				  "suppliers": ["%s"]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests/{id}/quotations", requestId)
						.contentType("application/json").content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/purchasing/requests/{id}/quotations", requestId)
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("OPEN")));
	}

	@Test
	void sendingAQuotationWithoutAnySupplierIsRejectedWith400() throws Exception {
		UUID requestId = createOpenPurchaseRequest();
		String body = """
				{
				  "suppliers": []
				}
				""";

		mockMvc.perform(post("/api/purchasing/requests/{id}/quotations", requestId)
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sendingAQuotationForAMissingRequestIsRejectedWith404() throws Exception {
		String body = """
				{
				  "suppliers": ["%s"]
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/requests/{id}/quotations", UUID.randomUUID())
						.contentType("application/json").content(body))
				.andExpect(status().isNotFound());
	}

	private UUID createOpenPurchaseRequest() throws Exception {
		String body = """
				{
				  "origin": "USER",
				  "requestedBy": "%s",
				  "items": [{"productId": "%s", "quantity": 10}]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		MvcResult result = mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(json.get("id").asString());
	}
}
