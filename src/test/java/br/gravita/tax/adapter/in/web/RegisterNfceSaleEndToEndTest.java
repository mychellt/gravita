package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceSaleJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.PosSessionJpaRepository;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * GRA-91: end-to-end verification of RegisterNfceSaleUseCase
 * (POST /api/pdv/sales) through the real HTTP stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RegisterNfceSaleEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PosSessionJpaRepository posSessionJpaRepository;

	@Autowired
	private NfceSaleJpaRepository nfceSaleJpaRepository;

	@Autowired
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void ac1and3and4_registersASaleWithMultiplePaymentsAndDerivesChange() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = UUID.randomUUID();

		String response = mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 80.00 } ],
								  "payments": [
								    { "method": "CASH", "amount": 50.00 },
								    { "method": "CREDIT_CARD", "amount": 32.00 }
								  ]
								}
								""".formatted(sessionId, productId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();

		UUID saleId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		NfceSaleJpaEntity persisted = nfceSaleJpaRepository.findById(saleId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(NfceSaleStatus.DRAFT);
		assertThat(persisted.getPayments()).hasSize(2);
		assertThat(persisted.getChangeGiven()).isEqualByComparingTo("2.00");
	}

	@Test
	void ac2_anItemDiscountExceedingTheLinkedPriceTablesBlockCapIsRejected() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID priceTableId = seedPriceTable("5", MaxDiscountBehavior.BLOCK);
		UUID productId = UUID.randomUUID();

		mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "priceTableId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 100.00, "itemDiscount": 20.00 } ],
								  "payments": [ { "method": "CASH", "amount": 80.00 } ]
								}
								""".formatted(sessionId, priceTableId, productId)))
				.andExpect(status().isConflict());
	}

	@Test
	void ac2_anItemDiscountExceedingTheLinkedPriceTablesAlertCapStillRegistersTheSale() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID priceTableId = seedPriceTable("5", MaxDiscountBehavior.ALERT);
		UUID productId = UUID.randomUUID();

		mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "priceTableId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 100.00, "itemDiscount": 20.00 } ],
								  "payments": [ { "method": "CASH", "amount": 80.00 } ]
								}
								""".formatted(sessionId, priceTableId, productId)))
				.andExpect(status().isCreated());
	}

	@Test
	void ac5_paymentsThatDoNotCoverTheSaleTotalAreRejected() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = UUID.randomUUID();

		mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 50.00 } ],
								  "payments": [ { "method": "CASH", "amount": 40.00 } ]
								}
								""".formatted(sessionId, productId)))
				.andExpect(status().isConflict());
	}

	@Test
	void ac6_aTypedCustomerCpfIsAccepted() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = UUID.randomUUID();

		String response = mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 10.00 } ],
								  "payments": [ { "method": "CASH", "amount": 10.00 } ],
								  "customerCpf": "529.982.247-25"
								}
								""".formatted(sessionId, productId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		UUID saleId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		assertThat(nfceSaleJpaRepository.findById(saleId).orElseThrow().getCustomerCpf()).isEqualTo("52998224725");
	}

	private UUID seedOpenSession() {
		UUID sessionId = UUID.randomUUID();
		posSessionJpaRepository.save(PosSessionJpaEntity.builder()
				.id(sessionId)
				.registerId(UUID.randomUUID())
				.operatorId(UUID.randomUUID())
				.openingChangeAmount(new BigDecimal("100.00"))
				.status(PosSessionStatus.OPEN)
				.openedAt(Instant.now())
				.build());
		return sessionId;
	}

	private UUID seedPriceTable(String maxDiscountPercent, MaxDiscountBehavior behavior) {
		PriceTable priceTable = priceTableRepositoryPort.save(PriceTable.of(PriceTableId.of(UUID.randomUUID()),
				PriceFormation.FIXED, LocalDate.now().minusDays(1), null, new BigDecimal(maxDiscountPercent), behavior,
				List.of()));
		return priceTable.getId().value();
	}
}
