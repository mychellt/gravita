package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RegisterQuotationResponseEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QuotationRepositoryPort quotationRepositoryPort;

	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();
	private final UUID supplierId = UUID.randomUUID();

	@Test
	void aSentSupplierCanRegisterAResponsePricingEveryItem() throws Exception {
		QuotationId quotationId = seedQuotation(supplierId);

		mockMvc.perform(post("/api/purchasing/quotations/" + quotationId.value() + "/responses")
						.contentType("application/json").content(responseBody(supplierId, LocalDate.now().plusDays(7))))
				.andExpect(status().isNoContent());
	}

	@Test
	void reSubmittingAResponseFromTheSameSupplierAlsoSucceeds() throws Exception {
		QuotationId quotationId = seedQuotation(supplierId);
		mockMvc.perform(post("/api/purchasing/quotations/" + quotationId.value() + "/responses")
				.contentType("application/json").content(responseBody(supplierId, LocalDate.now().plusDays(7))));

		mockMvc.perform(post("/api/purchasing/quotations/" + quotationId.value() + "/responses")
						.contentType("application/json").content(responseBody(supplierId, LocalDate.now().plusDays(2))))
				.andExpect(status().isNoContent());
	}

	@Test
	void aSupplierNotSentTheQuotationIsRejectedWith400() throws Exception {
		QuotationId quotationId = seedQuotation(supplierId);
		UUID strangerSupplier = UUID.randomUUID();

		mockMvc.perform(post("/api/purchasing/quotations/" + quotationId.value() + "/responses")
						.contentType("application/json").content(responseBody(strangerSupplier, LocalDate.now())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aResponseMissingAnItemIsRejectedWith400() throws Exception {
		QuotationId quotationId = seedQuotation(supplierId);
		String body = """
				{
				  "supplierId": "%s",
				  "itemPrices": [{"productId": "%s", "unitPrice": 10}],
				  "deadline": "%s"
				}
				""".formatted(supplierId, productA, LocalDate.now().plusDays(1));

		mockMvc.perform(post("/api/purchasing/quotations/" + quotationId.value() + "/responses")
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registeringAResponseAgainstAnUnknownQuotationIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/purchasing/quotations/" + UUID.randomUUID() + "/responses")
						.contentType("application/json").content(responseBody(supplierId, LocalDate.now())))
				.andExpect(status().isNotFound());
	}

	private String responseBody(UUID supplierId, LocalDate deadline) {
		return """
				{
				  "supplierId": "%s",
				  "itemPrices": [
				    {"productId": "%s", "unitPrice": 10},
				    {"productId": "%s", "unitPrice": 5}
				  ],
				  "deadline": "%s"
				}
				""".formatted(supplierId, productA, productB, deadline);
	}

	private QuotationId seedQuotation(UUID sentSupplierId) {
		QuotationId id = QuotationId.of(UUID.randomUUID());
		Quotation quotation = Quotation.send(id, PurchaseRequestId.of(UUID.randomUUID()),
				List.of(new QuotationItem(productA, BigDecimal.TEN), new QuotationItem(productB, BigDecimal.valueOf(5))),
				List.of(SupplierId.of(sentSupplierId)));
		return quotationRepositoryPort.save(quotation).getId();
	}
}
