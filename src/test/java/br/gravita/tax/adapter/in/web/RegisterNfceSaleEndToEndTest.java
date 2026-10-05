package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.TaxRateRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceSaleJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.PosSessionJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.TaxRateRuleJpaRepository;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RegisterNfceSaleEndToEndTest {

	private static final String NCM = "85171231";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PosSessionJpaRepository posSessionJpaRepository;

	@Autowired
	private NfceSaleJpaRepository nfceSaleJpaRepository;

	@Autowired
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;

	@Autowired
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	@Autowired
	private CertificateStoragePort certificateStoragePort;

	@Autowired
	private ProductJpaRepository productJpaRepository;

	@Autowired
	private TaxRateRuleJpaRepository taxRateRuleJpaRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private CompanyId companyId;

	@BeforeEach
	void seedIssuanceInfrastructure() throws Exception {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, "Acme Ltda", Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfce@example.com", "11999999999", null, null));
		documentSeriesRepositoryPort
				.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFCE).reconfigure("001", 1L));
		certificateStoragePort.save(DigitalCertificate.upload(companyId, CertificateType.A1, loadCertificateFixture(),
				"gravita-test-pass", Instant.now().plusSeconds(3600)));
		taxRateRuleJpaRepository.save(taxRateRule());
	}

	@Test
	@DisplayName("Registers a sale with multiple payments and derives the change")
	void ac1and3and4_registersASaleWithMultiplePaymentsAndDerivesChange() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = seedProduct();

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
		assertThat(persisted.getStatus()).isEqualTo(NfceSaleStatus.PENDING_SYNC);
		assertThat(persisted.isContingencyMode()).isTrue();
		assertThat(persisted.getAccessKey()).hasSize(44);
		assertThat(persisted.getPayments()).hasSize(2);
		assertThat(persisted.getChangeGiven()).isEqualByComparingTo("2.00");
	}

	@Test
	@DisplayName("Rejects an item discount above the linked price table's blocking cap")
	void ac2_anItemDiscountExceedingTheLinkedPriceTablesBlockCapIsRejected() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID priceTableId = seedPriceTable("5", MaxDiscountBehavior.BLOCK);
		UUID productId = seedProduct();

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
	@DisplayName("Still registers the sale when an item discount exceeds only the price table's alert cap")
	void ac2_anItemDiscountExceedingTheLinkedPriceTablesAlertCapStillRegistersTheSale() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID priceTableId = seedPriceTable("5", MaxDiscountBehavior.ALERT);
		UUID productId = seedProduct();

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
	@DisplayName("Rejects payments that do not cover the sale total")
	void ac5_paymentsThatDoNotCoverTheSaleTotalAreRejected() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = seedProduct();

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
	@DisplayName("Accepts a customer CPF typed at the register")
	void ac6_aTypedCustomerCpfIsAccepted() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = seedProduct();

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
				.companyId(companyId.value())
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

	private UUID seedProduct() {
		UUID productId = UUID.randomUUID();
		ProductJpaEntity entity = ProductJpaEntity.builder()
				.id(productId)
				.internalCode("SKU-" + productId)
				.type(ProductType.SIMPLE)
				.barcodes(List.of())
				.images(List.of())
				.status(ProductStatus.ACTIVE)
				.ncm(NCM)
				.build();
		entity.setNew(true);
		productJpaRepository.save(entity);
		return productId;
	}

	private TaxRateRuleJpaEntity taxRateRule() {
		TaxRateRuleJpaEntity entity = TaxRateRuleJpaEntity.builder()
				.id(UUID.randomUUID())
				.ncm(NCM)
				.originState("SP")
				.destinationState("SP")
				.regime(br.gravita.core.domain.tax.TaxRegime.SIMPLES_NACIONAL)
				.operationType("VENDA_PDV")
				.taxType(TaxType.ICMS)
				.ratePercentage(new BigDecimal("18.0000"))
				.baseReductionPercentage(BigDecimal.ZERO)
				.mvaPercentage(BigDecimal.ZERO)
				.build();
		entity.setNew(true);
		return entity;
	}

	private byte[] loadCertificateFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/certificates/test-a1.pfx")) {
			return in.readAllBytes();
		}
	}
}
