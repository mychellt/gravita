package br.gravita.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs the real controller, service, read-model adapter and PDF renderer over the real repositories; only session and permission are stubbed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetFiscalBooksEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NfeRepositoryPort nfeRepositoryPort;

	@Autowired
	private NfceRepositoryPort nfceRepositoryPort;

	@Autowired
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final CompanyId company = CompanyId.of(UUID.randomUUID());
	private NfeDocument authorizedNfe;
	private NfceSale authorizedNfce;

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("fiscal-books")))).thenReturn(true);

		// Inside March 2018 (a month long past, so other tests' documents cannot collide), edges included.
		inbound("1", "100", 1, 0, 0, "Fornecedor Alfa", "1102", "1403", "165.00", "27.00");
		inbound("1", "101", 31, 23, 30, "Fornecedor Beta", "1102", "1102", "50.00", "0");
		// Outside it.
		inbound("1", "102", 28, 23, 30, "Fornecedor Fevereiro", "1102", "1102", "999.00", "99.00", 2);
		inbound("1", "103", 1, 0, 0, "Fornecedor Abril", "1102", "1102", "999.00", "99.00", 4);

		authorizedNfe = nfe(NfeDocumentStatus.AUTHORIZED, 10, 3, "1", 20L);
		nfeRepositoryPort.save(authorizedNfe);
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.REJECTED, null, 3, "1", 21L));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.CANCELLED, 11, 3, "1", 22L));
		nfeRepositoryPort.save(nfe(NfeDocumentStatus.AUTHORIZED, 1, 4, "1", 23L));

		authorizedNfce = nfce(NfceSaleStatus.AUTHORIZED, 15, 3, 7L);
		nfceRepositoryPort.save(authorizedNfce);
		nfceRepositoryPort.save(nfce(NfceSaleStatus.PENDING_SYNC, 16, 3, 8L));
		nfceRepositoryPort.save(nfce(NfceSaleStatus.AUTHORIZED, 17, 4, 9L));
	}

	@DisplayName("Books the period's authorized and received documents and assesses ICMS")
	@Test
	void booksTheAuthorizedAndReceivedDocumentsOfThePeriodAndAssessesIcms() throws Exception {
		mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-03").header("Authorization",
				"Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("2018-03"))
				.andExpect(jsonPath("$.entries.length()").value(2))
				.andExpect(jsonPath("$.entries[0].flow").value("ENTRY"))
				.andExpect(jsonPath("$.entries[0].date").value("2018-03-01"))
				.andExpect(jsonPath("$.entries[0].counterpartName").value("Fornecedor Alfa"))
				.andExpect(jsonPath("$.entries[0].cfop").value("1102/1403"))
				.andExpect(jsonPath("$.entries[0].totalValue").value(165.0))
				.andExpect(jsonPath("$.entries[0].icmsValue").value(27.0))
				.andExpect(jsonPath("$.entries[1].date").value("2018-03-31"))
				.andExpect(jsonPath("$.entries[1].cfop").value("1102"))
				.andExpect(jsonPath("$.exits.length()").value(2))
				.andExpect(jsonPath("$.exits[0].documentModel").value("NFE"))
				.andExpect(jsonPath("$.exits[0].date").value("2018-03-10"))
				.andExpect(jsonPath("$.exits[0].number").value("20"))
				.andExpect(jsonPath("$.exits[0].cfop").value("5102"))
				.andExpect(jsonPath("$.exits[0].counterpartName").value("Cliente SA"))
				.andExpect(jsonPath("$.exits[0].icmsValue").value(18.0))
				.andExpect(jsonPath("$.exits[1].documentModel").value("NFCE"))
				.andExpect(jsonPath("$.exits[1].date").value("2018-03-15"))
				.andExpect(jsonPath("$.exits[1].icmsValue").value(0))
				.andExpect(jsonPath("$.icmsAssessment.length()").value(2))
				.andExpect(jsonPath("$.icmsAssessment[0].flow").value("EXIT"))
				.andExpect(jsonPath("$.icmsAssessment[1].flow").value("ENTRY"))
				.andExpect(jsonPath("$.icmsDebit").value(18.0))
				.andExpect(jsonPath("$.icmsCredit").value(27.0))
				.andExpect(jsonPath("$.icmsBalance").value(-9.0));
	}

	@DisplayName("Reconciles the exit book against the period's authorized NF-e and NFC-e records")
	@Test
	void reconcilesTheExitsAgainstTheAuthorizedNfeAndNfceRecordsOfThePeriod() throws Exception {
		String body = mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-03").header(
				"Authorization", "Bearer valid-token")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		List<Double> booked = JsonPath.read(body, "$.exits[*].totalValue");
		BigDecimal bookedTotal = booked.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal authorizedTotal = authorizedNfe.getDocumentTotal().add(authorizedNfce.getSaleTotal());
		assertThat(bookedTotal).isEqualByComparingTo(authorizedTotal);
		List<String> accessKeys = JsonPath.read(body, "$.exits[*].accessKey");
		assertThat(accessKeys).containsExactly(authorizedNfe.getAccessKey(), authorizedNfce.getAccessKey());
	}

	@DisplayName("Renders the PDF and the TXT with the same books")
	@Test
	void rendersThePdfAndTheTxtWithTheSameBooks() throws Exception {
		String body = mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-03").header(
				"Authorization", "Bearer valid-token")).andExpect(status().isOk())
				.andExpect(jsonPath("$.pdf").value(startsWith("JVBERi"))).andReturn().getResponse()
				.getContentAsString();

		String txt = new String(Base64.getDecoder().decode((String) JsonPath.read(body, "$.txt")),
				StandardCharsets.UTF_8);
		assertThat(txt).containsSubsequence("LIVROS FISCAIS - 03/2018", "LIVRO DE ENTRADAS", "Fornecedor Alfa",
				"Fornecedor Beta", "Documentos: 2 | Valor total: 215,00 | ICMS: 27,00", "LIVRO DE SAÍDAS",
				"Cliente SA", "NFCE", "LIVRO DE APURAÇÃO DO ICMS", "Débitos (saídas): 18,00",
				"Créditos (entradas): 27,00", "Saldo (débitos - créditos): -9,00");
		assertThat(txt).doesNotContain("Fevereiro", "Abril");
	}

	@DisplayName("Returns empty books for a period without documents")
	@Test
	void answersEmptyBooksForAPeriodWithoutDocuments() throws Exception {
		mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-01").header("Authorization",
				"Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entries.length()").value(0))
				.andExpect(jsonPath("$.exits.length()").value(0))
				.andExpect(jsonPath("$.icmsAssessment.length()").value(0))
				.andExpect(jsonPath("$.icmsBalance").value(0))
				.andExpect(jsonPath("$.pdf").value(startsWith("JVBERi")));
	}

	@DisplayName("Returns 400 when the period is missing or malformed")
	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/fiscal-books").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/fiscal-books").param("period", "March").header("Authorization",
				"Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@DisplayName("Returns 403 when the profile cannot view the fiscal books")
	@Test
	void answersForbiddenWhenTheProfileCannotViewTheBooks() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-03").header("Authorization",
				"Bearer other-token")).andExpect(status().isForbidden());
	}

	@DisplayName("Returns 401 when there is no session")
	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/fiscal-books").param("period", "2018-03"))
				.andExpect(status().isUnauthorized());
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2018, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private void inbound(String series, String number, int day, int hour, int minute, String supplier, String cfopA,
			String cfopB, String total, String icms) {
		inbound(series, number, day, hour, minute, supplier, cfopA, cfopB, total, icms, 3);
	}

	private void inbound(String series, String number, int day, int hour, int minute, String supplier, String cfopA,
			String cfopB, String total, String icms, int month) {
		BigDecimal totalValue = new BigDecimal(total);
		BigDecimal icmsValue = new BigDecimal(icms);
		InboundNfeItem a = new InboundNfeItem("SKU-A", "Item A", "73181500", cfopA, "UN", BigDecimal.ONE, totalValue,
				totalValue, icmsValue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		InboundNfeItem b = new InboundNfeItem("SKU-B", "Item B", "73181500", cfopB, "UN", BigDecimal.ONE,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		InboundNfeTotals totals = new InboundNfeTotals(totalValue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, icmsValue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, totalValue);
		inboundNfeRepositoryPort.save(InboundNfe.of(InboundNfeId.of(UUID.randomUUID()), company,
				"35180" + "1".repeat(4) + String.format("%035d", Long.parseLong(number) + month * 1000L), series, number,
				Document.cnpj("11222333000181"), supplier, at(month, day, hour, minute), List.of(a, b), totals,
				"xml-ref", InboundNfeStatus.PENDING_CONFERENCE, at(month, day, hour, minute)));
	}

	private NfeDocument nfe(NfeDocumentStatus status, Integer day, int month, String series, long number) {
		TaxLineBreakdown icms = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", List.of(icms));
		NfeItem item = new NfeItem(UUID.randomUUID(), "Arroz", BigDecimal.TEN, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		Instant authorizedAt = day == null ? null : at(month, day, 12, 0);
		Instant cancelledAt = status == NfeDocumentStatus.CANCELLED ? at(month, day + 1, 12, 0) : null;
		return NfeDocument.of(NfeDocumentId.of(UUID.randomUUID()), company, null, NaturezaOperacao.VENDA,
				new Cfop("5102"), recipient, List.of(item), new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO,
				null, null, null, TaxCalculationTotals.from(List.of(breakdown)), status, at(month, 1, 8, 0), series,
				number, "35" + String.format("%042d", number + month * 100L), "protocol", false, null, null, null,
				List.of(), authorizedAt, cancelledAt == null ? null : "Cancelada a pedido", cancelledAt);
	}

	private NfceSale nfce(NfceSaleStatus status, int day, int month, long number) {
		SaleItem item = new SaleItem(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("20.00"), BigDecimal.ZERO);
		return NfceSale.of(NfceSaleId.of(UUID.randomUUID()), PosSessionId.of(UUID.randomUUID()), List.of(item), null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("40.00"))), BigDecimal.ZERO, null, status,
				at(month, day, 12, 0), "1", number, "35" + String.format("%042d", number + month * 100L + 5_000), "protocol",
				false);
	}
}
