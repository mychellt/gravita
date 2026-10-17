package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.PaymentMethodType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.tax.controllers.InboundNfeController;
import br.gravita.adapters.inbound.controllers.tax.controllers.NfeController;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.tax.CancelNfeUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyUseCase;
import br.gravita.core.ports.inbound.tax.GetInboundNfeConferenceUseCase;
import br.gravita.core.ports.inbound.tax.GetProductPurchaseHistoryUseCase;
import br.gravita.core.domain.tax.ProductPurchase;
import br.gravita.core.ports.inbound.tax.InboundNfeConference;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.inbound.tax.ListInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ListNfeUseCase;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemUseCase;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.ports.outbound.tax.GenerateDanfePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ NfeController.class, InboundNfeController.class })
@Import(WebMvcConfiguration.class)
class NfeListControllerTest {

	private static final String SESSION_TOKEN = "session-token";
	private static final String BEARER = "Bearer " + SESSION_TOKEN;
	private static final PaymentMethodType PAYMENT_METHOD_TYPE = PaymentMethodType.PIX;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListNfeUseCase listNfeUseCase;
	@MockitoBean
	private ListInboundNfeUseCase listInboundNfeUseCase;
	@MockitoBean
	private GetInboundNfeConferenceUseCase getInboundNfeConferenceUseCase;
	@MockitoBean
	private ResolveInboundNfeItemUseCase resolveInboundNfeItemUseCase;
	@MockitoBean
	private GetProductPurchaseHistoryUseCase getProductPurchaseHistoryUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private IssueNfeUseCase issueNfeUseCase;
	@MockitoBean
	private ResendNfeEmailUseCase resendNfeEmailUseCase;
	@MockitoBean
	private IssueCorrectionLetterUseCase issueCorrectionLetterUseCase;
	@MockitoBean
	private CancelNfeUseCase cancelNfeUseCase;
	@MockitoBean
	private NfeRepositoryPort nfeRepositoryPort;
	@MockitoBean
	private CompanyRepositoryPort companyRepositoryPort;
	@MockitoBean
	private XmlObjectStoragePort xmlObjectStoragePort;
	@MockitoBean
	private GenerateDanfePort generateDanfePort;
	@MockitoBean
	private ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase;
	@MockitoBean
	private EnterInboundNfeManuallyUseCase enterInboundNfeManuallyUseCase;
	@MockitoBean
	private ConfirmInboundNfeReceiptUseCase confirmInboundNfeReceiptUseCase;
	@MockitoBean
	private ManifestInboundNfeUseCase manifestInboundNfeUseCase;

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve(SESSION_TOKEN)).thenReturn(Optional.of(UserId.generate()));
	}

	@Test
	@DisplayName("GET /api/nfe lists the caller's NF-e with total, ICMS and status")
	void shouldListNfe() throws Exception {
		when(listNfeUseCase.execute(any())).thenReturn(List.of(draftNfe()));

		mockMvc.perform(get("/api/nfe").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].recipientName").value("Cliente PJ Teste"))
				.andExpect(jsonPath("$[0].cfop").value("5102"))
				.andExpect(jsonPath("$[0].status").value("DRAFT"))
				.andExpect(jsonPath("$[0].totalValue").value(115.00))
				.andExpect(jsonPath("$[0].icmsValue").value(18.00))
				.andExpect(jsonPath("$[0].emittedAt").exists())
				.andExpect(jsonPath("$[0].paymentMethodType").value(PAYMENT_METHOD_TYPE.name()));
	}

	@Test
	@DisplayName("GET /api/nfe returns an empty array for a company without NF-e")
	void shouldListNothing() throws Exception {
		when(listNfeUseCase.execute(any())).thenReturn(List.of());

		mockMvc.perform(get("/api/nfe").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("GET /api/nfe/inbound lists the caller's inbound NF-e")
	void shouldListInboundNfe() throws Exception {
		when(listInboundNfeUseCase.execute(any())).thenReturn(List.of(inboundNfe()));

		mockMvc.perform(get("/api/nfe/inbound").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].supplierName").value("Fornecedor Exemplo LTDA"))
				.andExpect(jsonPath("$[0].series").value("1"))
				.andExpect(jsonPath("$[0].number").value("12345"))
				.andExpect(jsonPath("$[0].totalValue").value(165.00))
				.andExpect(jsonPath("$[0].status").value("PENDING_CONFERENCE"));
	}

	@Test
	@DisplayName("GET /api/nfe/inbound/{id}/conference returns the supplier resolution and each item's linked product")
	void shouldReturnTheInboundNfeConference() throws Exception {
		final UUID inboundNfeId = UUID.randomUUID();
		final UUID productId = UUID.randomUUID();
		when(getInboundNfeConferenceUseCase.execute(any())).thenReturn(new InboundNfeConference(inboundNfeId, "11222333000181",
				"Fornecedor Alfa", true, UUID.randomUUID(), List.of(new InboundNfeConference.Item(0, "SKU-001", "Parafuso", "73181500", "UN",
						BigDecimal.TEN, new BigDecimal("1.8000"), productId, "PARAF-M8", new BigDecimal("1.5000"),
						Instant.parse("2026-01-10T12:00:00Z"), new BigDecimal("0.3000")))));

		mockMvc.perform(get("/api/nfe/inbound/" + inboundNfeId + "/conference").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.supplierResolved").value(true))
				.andExpect(jsonPath("$.supplierDocument").value("11222333000181"))
				.andExpect(jsonPath("$.supplierName").value("Fornecedor Alfa"))
				.andExpect(jsonPath("$.items[0].resolved").value(true))
				.andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
				.andExpect(jsonPath("$.items[0].lastPurchaseUnitValue").value(1.5))
				.andExpect(jsonPath("$.items[0].priceDelta").value(0.3));
	}

	@Test
	@DisplayName("GET /api/nfe/inbound/purchase-history returns what was paid for the product, supplier by supplier")
	void shouldReturnTheProductPurchaseHistory() throws Exception {
		final UUID productId = UUID.randomUUID();
		when(getProductPurchaseHistoryUseCase.execute(any())).thenReturn(List.of(
				new ProductPurchase(UUID.randomUUID(), "1", "109887", "19117785000105", "Fornecedor A",
						Instant.parse("2026-01-10T12:00:00Z"), BigDecimal.ONE, new BigDecimal("6998.89")),
				new ProductPurchase(UUID.randomUUID(), "1", "5", "11222333000181", "Fornecedor B",
						Instant.parse("2025-12-01T12:00:00Z"), BigDecimal.TEN, new BigDecimal("6500.00"))));

		mockMvc.perform(get("/api/nfe/inbound/purchase-history").param("productId", productId.toString())
						.header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].supplierName").value("Fornecedor A"))
				.andExpect(jsonPath("$[0].unitValue").value(6998.89))
				.andExpect(jsonPath("$[1].supplierName").value("Fornecedor B"));
	}

	@Test
	@DisplayName("POST /api/nfe/inbound/{id}/items/{index}/link-product links an existing product to the item")
	void shouldLinkAnExistingProductToTheItem() throws Exception {
		final UUID inboundNfeId = UUID.randomUUID();
		final UUID productId = UUID.randomUUID();
		when(resolveInboundNfeItemUseCase.execute(any(ResolveInboundNfeItemCommand.class))).thenReturn(
				SupplierProductCatalogLink.create(CompanyId.of(UUID.randomUUID()), SupplierId.of(UUID.randomUUID()),
						"SKU-001", productId, new BigDecimal("1.5000"), Instant.parse("2026-01-10T12:00:00Z")));

		mockMvc.perform(post("/api/nfe/inbound/" + inboundNfeId + "/items/0/link-product")
				.header("Authorization", BEARER).contentType("application/json")
				.content("{\"productId\":\"" + productId + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productId").value(productId.toString()))
				.andExpect(jsonPath("$.supplierProductCode").value("SKU-001"));
	}

	private NfeDocument draftNfe() {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown icms = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(icms));
		final NfeItem item = new NfeItem(productId, "Produto", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
				breakdown);
		return NfeDocument.draft().id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(CompanyId.of(UUID.randomUUID())).naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81", PersonType.COMPANY,
						"Cliente PJ Teste", "123456789", "RJ"))
				.items(List.of(item)).freight(new BigDecimal("15.00")).insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO).paymentMethodType(PAYMENT_METHOD_TYPE).taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
				.createdAt(Instant.parse("2028-02-01T10:00:00Z")).build();
	}

	private InboundNfe inboundNfe() {
		final InboundNfeItem item = new InboundNfeItem("SKU-001", "Parafuso", "73181500", "5102", "UN",
				new BigDecimal("10"), new BigDecimal("1.5000"), new BigDecimal("150.00"), new BigDecimal("27.00"),
				BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"));
		final InboundNfeTotals totals = new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), null, null,
				null, new BigDecimal("27.00"), null, null, null, new BigDecimal("165.00"));
		return InboundNfe.importedFromXml().id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(CompanyId.of(UUID.randomUUID())).accessKey("35240111222333000181550010000012345123456789")
				.series("1").number("12345").supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA").issuedAt(Instant.parse("2028-01-15T00:00:00Z"))
				.items(List.of(item)).totals(totals).xmlStorageRef("xml-ref-1").build();
	}
}
