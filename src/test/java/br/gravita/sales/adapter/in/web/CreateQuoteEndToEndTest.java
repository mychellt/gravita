package br.gravita.sales.adapter.in.web;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteStatus;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CreateQuoteEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private QuoteRepositoryPort quoteRepositoryPort;

	@Test
	void aQuoteIsPersistedAsDraftWithItsItemsPricesDiscountsAndValidityAsSubmitted() throws Exception {
		UUID customerId = UUID.randomUUID();
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		LocalDate validUntil = LocalDate.now().plusDays(15);

		String body = """
				{
				  "customerId": "%s",
				  "salespersonId": "%s",
				  "validUntil": "%s",
				  "items": [
				    {"productOrServiceId": "%s", "quantity": 3, "unitPrice": 12.3456, "discount": 1.50},
				    {"productOrServiceId": "%s", "quantity": 1, "unitPrice": 250.00}
				  ]
				}
				""".formatted(customerId, salespersonId, validUntil, productId, serviceId);

		String response = mockMvc.perform(post("/api/sales/quotes").contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.customerId").value(customerId.toString()))
				.andExpect(jsonPath("$.validUntil").value(validUntil.toString()))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].productOrServiceId").value(productId.toString()))
				.andExpect(jsonPath("$.items[1].productOrServiceId").value(serviceId.toString()))
				.andReturn().getResponse().getContentAsString();

		UUID quoteId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		Quote saved = quoteRepositoryPort.findById(QuoteId.of(quoteId)).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(QuoteStatus.DRAFT);
		assertThat(saved.getCustomerId()).isEqualTo(customerId);
		assertThat(saved.getValidUntil()).isEqualTo(validUntil);
		assertThat(saved.getItems()).hasSize(2);

		QuoteItem first = saved.getItems().get(0);
		assertThat(first.productOrServiceId()).isEqualTo(productId);
		assertThat(first.quantity()).isEqualByComparingTo("3");
		assertThat(first.unitPrice()).isEqualByComparingTo("12.3456");
		assertThat(first.discount()).isEqualByComparingTo("1.50");

		QuoteItem second = saved.getItems().get(1);
		assertThat(second.productOrServiceId()).isEqualTo(serviceId);
		assertThat(second.unitPrice()).isEqualByComparingTo("250.00");
		assertThat(second.discount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void aQuoteWithNoItemsIsRejectedWith400() throws Exception {
		String body = """
				{"customerId": "%s", "validUntil": "%s", "items": []}
				""".formatted(UUID.randomUUID(), LocalDate.now().plusDays(1));

		mockMvc.perform(post("/api/sales/quotes").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aValidityDateInThePastIsRejectedWith400() throws Exception {
		mockMvc.perform(post("/api/sales/quotes").contentType("application/json")
						.content(singleItemBody(LocalDate.now().minusDays(1))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("in the future")));
	}

	@Test
	void aValidityDateOfTodayIsRejectedWith400() throws Exception {
		mockMvc.perform(post("/api/sales/quotes").contentType("application/json")
						.content(singleItemBody(LocalDate.now())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aDiscountAboveTheLineSubtotalIsRejectedWith400() throws Exception {
		String body = """
				{
				  "customerId": "%s",
				  "validUntil": "%s",
				  "items": [{"productOrServiceId": "%s", "quantity": 1, "unitPrice": 10, "discount": 10.01}]
				}
				""".formatted(UUID.randomUUID(), LocalDate.now().plusDays(1), UUID.randomUUID());

		mockMvc.perform(post("/api/sales/quotes").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aPriceWithMorePrecisionThanCanBeStoredIsRejectedRatherThanRounded() throws Exception {
		String body = """
				{
				  "customerId": "%s",
				  "validUntil": "%s",
				  "items": [{"productOrServiceId": "%s", "quantity": 1, "unitPrice": 10.12345}]
				}
				""".formatted(UUID.randomUUID(), LocalDate.now().plusDays(1), UUID.randomUUID());

		mockMvc.perform(post("/api/sales/quotes").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aMissingCustomerIsRejectedWith400() throws Exception {
		String body = """
				{
				  "validUntil": "%s",
				  "items": [{"productOrServiceId": "%s", "quantity": 1, "unitPrice": 10}]
				}
				""".formatted(LocalDate.now().plusDays(1), UUID.randomUUID());

		mockMvc.perform(post("/api/sales/quotes").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	private String singleItemBody(LocalDate validUntil) {
		return """
				{
				  "customerId": "%s",
				  "salespersonId": "%s",
				  "validUntil": "%s",
				  "items": [{"productOrServiceId": "%s", "quantity": 1, "unitPrice": 10}]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID(), validUntil, UUID.randomUUID());
	}
}
