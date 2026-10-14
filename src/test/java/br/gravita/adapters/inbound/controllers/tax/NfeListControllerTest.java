package br.gravita.adapters.inbound.controllers.tax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.inbound.tax.ListInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ListNfeUseCase;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailUseCase;
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

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListNfeUseCase listNfeUseCase;
	@MockitoBean
	private ListInboundNfeUseCase listInboundNfeUseCase;
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
				.andExpect(jsonPath("$[0].emittedAt").exists());
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
				.otherExpenses(BigDecimal.ZERO).taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
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
