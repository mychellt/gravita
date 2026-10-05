package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.tax.LivrosFiscaisFixtures;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real controller, service, repositories and renderers together: the books of one company's month are
 * built from what {@code tax} stores, and nothing of another company or of another month leaks into them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GenerateLivrosFiscaisEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;

	@Autowired
	private NfeRepositoryPort nfeRepositoryPort;

	@Autowired
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Autowired
	private VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;

	private final CompanyId company = CompanyId.of(UUID.randomUUID());
	private final CompanyId otherCompany = CompanyId.of(UUID.randomUUID());
	private NfeDocument authorizedNfe;

	@BeforeEach
	void seed() {
		saveCompany(company, "11.222.333/0001-81");
		saveCompany(otherCompany, "11.444.777/0001-61");

		// Inside March 2018 (a month long past, so other tests' documents cannot collide), edges included.
		inbound(company, "100", at(3, 1, 0, 0), "Fornecedor Alfa", "1102", "1403", "165.00", "27.00");
		inbound(company, "101", at(3, 31, 23, 30), "Fornecedor Beta", "1102", "1102", "50.00", "0");
		// Outside it, or another company's.
		inbound(company, "102", at(2, 28, 23, 30), "Fornecedor Fevereiro", "1102", "1102", "999.00", "99.00");
		inbound(company, "103", at(4, 1, 0, 0), "Fornecedor Abril", "1102", "1102", "999.00", "99.00");
		inbound(otherCompany, "104", at(3, 5, 9, 0), "Fornecedor Alheio", "1102", "1102", "999.00", "99.00");

		authorizedNfe = nfe(company, NfeDocumentStatus.AUTHORIZED, "5102", 20L, at(3, 10, 12, 0));
		nfeRepositoryPort.save(authorizedNfe);
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.REJECTED, "5102", 21L, null));
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.CANCELLED, "5102", 22L, at(3, 11, 12, 0)));
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.AUTHORIZED, "5102", 23L, at(4, 1, 12, 0)));
		nfeRepositoryPort.save(nfe(otherCompany, NfeDocumentStatus.AUTHORIZED, "5102", 24L, at(3, 10, 12, 0)));

		voided(company, "1", 101L, 110L, "formulários danificados", at(3, 12, 15, 0));
		voided(company, "1", 111L, 111L, "fora do período", at(4, 2, 15, 0));
		voided(otherCompany, "1", 1L, 5L, "de outra empresa", at(3, 12, 15, 0));
	}

	@Test
	@DisplayName("Books the company's documents of the period and assesses ICMS")
	void booksTheCompanysDocumentsOfThePeriodAndAssessesIcms() throws Exception {
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString()).param("period",
				"2018-03"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.companyId").value(company.value().toString()))
				.andExpect(jsonPath("$.period").value("2018-03"))
				.andExpect(jsonPath("$.entryBook.lines.length()").value(2))
				.andExpect(jsonPath("$.entryBook.lines[0].flow").value("ENTRY"))
				.andExpect(jsonPath("$.entryBook.lines[0].date").value("2018-03-01"))
				.andExpect(jsonPath("$.entryBook.lines[0].counterpartName").value("Fornecedor Alfa"))
				.andExpect(jsonPath("$.entryBook.lines[0].cfop").value("1102/1403"))
				.andExpect(jsonPath("$.entryBook.lines[0].totalValue").value(165.0))
				.andExpect(jsonPath("$.entryBook.lines[0].icmsValue").value(27.0))
				.andExpect(jsonPath("$.entryBook.lines[1].date").value("2018-03-31"))
				.andExpect(jsonPath("$.entryBook.totalValue").value(215.0))
				.andExpect(jsonPath("$.exitBook.lines.length()").value(1))
				.andExpect(jsonPath("$.exitBook.lines[0].flow").value("EXIT"))
				.andExpect(jsonPath("$.exitBook.lines[0].date").value("2018-03-10"))
				.andExpect(jsonPath("$.exitBook.lines[0].number").value("20"))
				.andExpect(jsonPath("$.exitBook.lines[0].cfop").value("5102"))
				.andExpect(jsonPath("$.exitBook.lines[0].counterpartName").value("Cliente SA"))
				.andExpect(jsonPath("$.exitBook.lines[0].icmsValue").value(18.0))
				.andExpect(jsonPath("$.icmsAssessmentBook.lines.length()").value(2))
				.andExpect(jsonPath("$.icmsAssessmentBook.lines[0].flow").value("EXIT"))
				.andExpect(jsonPath("$.icmsAssessmentBook.lines[1].flow").value("ENTRY"))
				.andExpect(jsonPath("$.icmsAssessmentBook.debit").value(18.0))
				.andExpect(jsonPath("$.icmsAssessmentBook.credit").value(27.0))
				.andExpect(jsonPath("$.icmsAssessmentBook.balance").value(-9.0));
	}

	@Test
	@DisplayName("Explains numbering gaps with the ranges the company voided in the period")
	void explainsTheNumberingGapsWithTheRangesTheCompanyVoidedInThePeriod() throws Exception {
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString()).param("period",
				"2018-03"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exitBook.voidedRanges.length()").value(1))
				.andExpect(jsonPath("$.exitBook.voidedRanges[0].series").value("1"))
				.andExpect(jsonPath("$.exitBook.voidedRanges[0].startNumber").value(101))
				.andExpect(jsonPath("$.exitBook.voidedRanges[0].endNumber").value(110))
				.andExpect(jsonPath("$.exitBook.voidedRanges[0].justification").value("formulários danificados"))
				.andExpect(jsonPath("$.exitBook.voidedRanges[0].sefazProtocol").value("void-protocol"))
				.andExpect(jsonPath("$.entryBook.voidedRanges.length()").value(0));
	}

	@Test
	@DisplayName("Summarises ICMS, IPI, PIS and COFINS of the period")
	void summarisesIcmsIpiPisAndCofinsOfThePeriod() throws Exception {
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString()).param("period",
				"2018-03"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taxSummary.icms.onExits").value(18.0))
				.andExpect(jsonPath("$.taxSummary.icms.onEntries").value(27.0))
				.andExpect(jsonPath("$.taxSummary.icms.balance").value(-9.0))
				.andExpect(jsonPath("$.taxSummary.ipi.onExits").value(5.0))
				.andExpect(jsonPath("$.taxSummary.pis.onExits").value(1.65))
				.andExpect(jsonPath("$.taxSummary.cofins.onExits").value(7.6))
				.andExpect(jsonPath("$.taxSummary.cofins.balance").value(7.6));
	}

	@Test
	@DisplayName("Reconciles the exit book against the period's authorized NF-e")
	void reconcilesTheExitBookAgainstTheAuthorizedNfeOfThePeriod() throws Exception {
		String body = mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString())
				.param("period", "2018-03")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		java.util.List<String> accessKeys = JsonPath.read(body, "$.exitBook.lines[*].accessKey");
		assertThat(accessKeys).containsExactly(authorizedNfe.getAccessKey());
		Double total = JsonPath.read(body, "$.exitBook.totalValue");
		assertThat(java.math.BigDecimal.valueOf(total)).isEqualByComparingTo(authorizedNfe.getDocumentTotal());
	}

	@Test
	@DisplayName("Renders the PDF and the TXT with the same books")
	void rendersThePdfAndTheTxtWithTheSameBooks() throws Exception {
		String body = mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString())
				.param("period", "2018-03")).andExpect(status().isOk())
				.andExpect(jsonPath("$.pdf").value(startsWith("JVBERi"))).andReturn().getResponse()
				.getContentAsString();

		String txt = new String(Base64.getDecoder().decode((String) JsonPath.read(body, "$.txt")),
				StandardCharsets.UTF_8);
		assertThat(txt).containsSubsequence("LIVROS FISCAIS - 03/2018", "CNPJ: 11222333000181",
				"Inscrição estadual: 123456789", "LIVRO DE ENTRADAS", "Fornecedor Alfa", "Fornecedor Beta",
				"Documentos: 2 | Valor total: 215,00", "LIVRO DE SAÍDAS", "Cliente SA",
				"LIVRO DE SAÍDAS - NUMERAÇÃO INUTILIZADA", "formulários danificados",
				"Faixas: 1 | Números inutilizados: 10", "LIVRO DE APURAÇÃO DO ICMS", "Débitos (saídas): 18,00",
				"Créditos (entradas): 27,00", "Saldo (débitos - créditos): -9,00", "RESUMO DE TRIBUTOS", "COFINS");
		assertThat(txt).doesNotContain("Fevereiro", "Abril", "Alheio", "fora do período", "de outra empresa");
	}

	@Test
	@DisplayName("Returns empty books for a period without documents")
	void answersEmptyBooksForAPeriodWithoutDocuments() throws Exception {
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", company.value().toString()).param("period",
				"2018-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entryBook.lines.length()").value(0))
				.andExpect(jsonPath("$.exitBook.lines.length()").value(0))
				.andExpect(jsonPath("$.exitBook.voidedRanges.length()").value(0))
				.andExpect(jsonPath("$.icmsAssessmentBook.lines.length()").value(0))
				.andExpect(jsonPath("$.icmsAssessmentBook.balance").value(0))
				.andExpect(jsonPath("$.pdf").value(startsWith("JVBERi")));
	}

	@Test
	@DisplayName("Returns 404 for an unknown company")
	void answersNotFoundForAnUnknownCompany() throws Exception {
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", UUID.randomUUID().toString()).param("period",
				"2018-03")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Returns 400 when the company or period is missing or malformed")
	void answersBadRequestWhenTheCompanyOrThePeriodIsMissingOrMalformed() throws Exception {
		String companyId = company.value().toString();
		mockMvc.perform(get("/api/livros-fiscais").param("period", "2018-03")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", companyId)).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", companyId).param("period", "March"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/livros-fiscais").param("companyId", "not-a-uuid").param("period", "2018-03"))
				.andExpect(status().isBadRequest());
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2018, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private void saveCompany(CompanyId id, String cnpj) {
		companyRepositoryPort.save(Company.of(id, "Acme Ltda", Document.cnpj(cnpj), "123456789", "987654",
				"6201500", TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null));
	}

	private void inbound(CompanyId owner, String number, Instant issuedAt, String supplier, String cfopA,
			String cfopB, String total, String icms) {
		inboundNfeRepositoryPort.save(LivrosFiscaisFixtures.receivedNfe(owner, "1", number, supplier, cfopA, cfopB,
				total, icms, "0", "0", "0", issuedAt));
	}

	private NfeDocument nfe(CompanyId issuer, NfeDocumentStatus status, String cfop, long number,
			Instant authorizedAt) {
		return LivrosFiscaisFixtures.issuedNfe(issuer, status, cfop, "1", number, authorizedAt);
	}

	private void voided(CompanyId owner, String series, long start, long end, String justification,
			Instant voidedAt) {
		voidedNumberRangeRepositoryPort.save(VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), owner,
				FiscalDocumentType.NFE, series, start, end, justification, "void-protocol", voidedAt));
	}
}
