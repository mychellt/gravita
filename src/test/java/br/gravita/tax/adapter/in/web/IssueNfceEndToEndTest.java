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
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class IssueNfceEndToEndTest {

	private static final String NCM = "85171231";
	private static HttpServer mockSefazServer;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PosSessionJpaRepository posSessionJpaRepository;

	@Autowired
	private NfceSaleJpaRepository nfceSaleJpaRepository;

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

	@DynamicPropertySource
	static void sefazBaseUrl(DynamicPropertyRegistry registry) throws Exception {
		mockSefazServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		mockSefazServer.createContext("/nfce/autorizacao", exchange -> {
			byte[] body = "{\"protocol\":\"protocol-e2e-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		mockSefazServer.start();
		int port = mockSefazServer.getAddress().getPort();
		registry.add("gravita.sefaz.base-url.homologation", () -> "http://localhost:" + port);
	}

	@AfterAll
	static void stopServer() {
		if (mockSefazServer != null) {
			mockSefazServer.stop(0);
		}
	}

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
		TaxRateRuleJpaEntity rule = TaxRateRuleJpaEntity.builder()
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
		rule.setNew(true);
		taxRateRuleJpaRepository.save(rule);
	}

	@Test
	@DisplayName("Authorizes the sale in real time with the protocol when SEFAZ is reachable")
	void ac1_aReachableSefazAuthorizesTheSaleInRealTimeWithTheProtocol() throws Exception {
		UUID sessionId = seedOpenSession();
		UUID productId = seedProduct();

		String response = mockMvc.perform(post("/api/pdv/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionId": "%s",
								  "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 10.00 } ],
								  "payments": [ { "method": "CASH", "amount": 10.00 } ]
								}
								""".formatted(sessionId, productId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("AUTHORIZED"))
				.andExpect(jsonPath("$.protocol").value("protocol-e2e-1"))
				.andExpect(jsonPath("$.accessKey").exists())
				.andReturn().getResponse().getContentAsString();

		UUID saleId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		NfceSaleJpaEntity persisted = nfceSaleJpaRepository.findById(saleId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(NfceSaleStatus.AUTHORIZED);
		assertThat(persisted.isContingencyMode()).isFalse();
		assertThat(persisted.getSefazProtocol()).isEqualTo("protocol-e2e-1");
		assertThat(persisted.getAccessKey()).hasSize(44);
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

	private byte[] loadCertificateFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/certificates/test-a1.pfx")) {
			return in.readAllBytes();
		}
	}
}
