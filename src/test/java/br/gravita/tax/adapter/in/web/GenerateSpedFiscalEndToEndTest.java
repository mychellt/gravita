package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import br.gravita.tax.SpedFiscalFixtures;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real controller, service, repositories and file writer together: one company's month becomes an EFD
 * ICMS/IPI file, and nothing of another company, of another month or of an unconfirmed receipt leaks into it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GenerateSpedFiscalEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final String TAXPAYER = """
			"taxpayer": {"legalName": "Empresa Teste Ltda", "municipalityCode": "3550308", "profile": "A",
			"activity": "OTHER", "tradeName": "Teste", "zipCode": "01310100", "number": "100",
			"neighborhood": "Bela Vista"}""";
	private static final String ACCOUNTANT = """
			"accountant": {"name": "Contador Teste", "cpf": "529.982.247-25", "crc": "SP-123456/O-0"}""";

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
	private NfeDocument cancelledNfe;

	@BeforeEach
	void seed() {
		saveCompany(company, "11.222.333/0001-81");
		saveCompany(otherCompany, "11.444.777/0001-61");

		// Inside March 2018 (a month long past, so other tests' documents cannot collide), edges included.
		inbound(company, "100", at(3, 1, 0, 0), "Fornecedor Alfa", "165.00", "27.00", true);
		inbound(company, "101", at(3, 31, 23, 30), "Fornecedor Beta", "50.00", "0", true);
		// Outside it, awaiting conference, or another company's.
		inbound(company, "102", at(2, 28, 23, 30), "Fornecedor Fevereiro", "999.00", "99.00", true);
		inbound(company, "103", at(4, 1, 0, 0), "Fornecedor Abril", "999.00", "99.00", true);
		inbound(company, "105", at(3, 6, 9, 0), "Fornecedor Pendente", "999.00", "99.00", false);
		inbound(otherCompany, "104", at(3, 5, 9, 0), "Fornecedor Alheio", "999.00", "99.00", true);

		authorizedNfe = nfe(company, NfeDocumentStatus.AUTHORIZED, "5102", 20L, at(3, 10, 12, 0));
		cancelledNfe = nfe(company, NfeDocumentStatus.CANCELLED, "5102", 22L, at(3, 11, 12, 0));
		nfeRepositoryPort.save(authorizedNfe);
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.REJECTED, "5102", 21L, null));
		nfeRepositoryPort.save(cancelledNfe);
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.AUTHORIZED, "5102", 23L, at(4, 1, 12, 0)));
		nfeRepositoryPort.save(nfe(otherCompany, NfeDocumentStatus.AUTHORIZED, "5102", 24L, at(3, 10, 12, 0)));

		voided(company, "1", 101L, 103L, at(3, 12, 15, 0));
		voided(company, "1", 111L, 111L, at(4, 2, 15, 0));
		voided(otherCompany, "1", 1L, 5L, at(3, 12, 15, 0));
	}

	@Test
	void generatesTheFileOfTheMonthWithEveryAuthorizedCancelledAndConfirmedDocument() throws Exception {
		List<String> lines = generate("\"period\": \"2018-03\"");

		assertThat(lines.get(0)).isEqualTo("|0000|020|0|01032018|31032018|Empresa Teste Ltda|11222333000181||SP|"
				+ "123456789|3550308|987654||A|1|");
		assertThat(lines).contains("|0001|0|", "|0100|Contador Teste|52998224725|SP-123456/O-0|||||||||||");
		// C100: the two received NFe, then the authorized exit, the cancelled one and the three voided numbers.
		List<String> c100 = lines.stream().filter(line -> line.startsWith("|C100|")).toList();
		assertThat(c100).hasSize(7);
		assertThat(c100.get(0)).startsWith("|C100|0|1|11222333000181|55|00|1|100|").contains("|01032018|01032018|165,00|");
		assertThat(c100.get(1)).startsWith("|C100|1|0|11222333000181|55|00|1|20|" + authorizedNfe.getAccessKey())
				.contains("|10032018|10032018|1015,00|2|0,00|0,00|1000,00|9|15,00|0,00|0,00|100,00|18,00|");
		assertThat(c100.get(2)).isEqualTo("|C100|1|0||55|02|1|22|" + cancelledNfe.getAccessKey() + "|");
		assertThat(c100.subList(3, 6)).containsExactly("|C100|1|0||55|05|1|101||", "|C100|1|0||55|05|1|102||",
				"|C100|1|0||55|05|1|103||");
		assertThat(c100.get(6)).startsWith("|C100|0|1|11222333000181|55|00|1|101|");
		assertThat(String.join("\n", lines)).doesNotContain("|C100|0|1|11222333000181|55|00|1|102|",
				"|105|", "|104|", "|1|23|", "|1|24|", "|1|21|", "|1|111|");
	}

	@Test
	void assessesTheIcmsAndKeepsBlocksAndCountersConsistent() throws Exception {
		List<String> lines = generate("\"period\": \"2018-03\"");

		assertThat(lines).contains("|E100|01032018|31032018|",
				"|E110|18,00|0,00|0,00|0,00|27,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|9,00|0,00|");
		assertThat(lines.stream().map(line -> line.substring(1, 5)).filter(reg -> reg.endsWith("001")).toList())
				.containsExactly("0001", "B001", "C001", "D001", "E001", "G001", "H001", "K001", "1001", "9001");
		assertThat(lines).contains("|B001|1|", "|B990|2|", "|D001|1|", "|G001|1|", "|H001|1|", "|K001|1|",
				"|1010|N|N|N|N|N|N|N|N|N|N|N|N|N|");
		assertThat(lines.get(lines.size() - 1)).isEqualTo("|9999|" + lines.size() + "|");
		// every X990 counts the lines of its block
		for (char block : new char[] {'B', 'C', 'D', 'E', 'G', 'H', 'K', '1'}) {
			long inBlock = lines.stream().filter(line -> line.charAt(1) == block).count();
			assertThat(lines).contains("|" + block + "990|" + inBlock + "|");
		}
	}

	@Test
	void respondsWithTheFileAsBase64LatinOneAndItsValidationReport() throws Exception {
		String body = mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON)
				.content(request("\"period\": \"2018-03\""))).andExpect(status().isOk())
				.andExpect(jsonPath("$.fileName").value("SPED-EFD-ICMS-IPI-11222333000181-2018-03.txt"))
				.andExpect(jsonPath("$.validation.issues[*].severity").value(hasItem("WARNING")))
				.andExpect(jsonPath("$.txt").value(startsWith("fDAwMDB"))).andReturn().getResponse()
				.getContentAsString();

		byte[] txt = Base64.getDecoder().decode((String) JsonPath.read(body, "$.txt"));
		assertThat(new String(txt, StandardCharsets.ISO_8859_1)).startsWith("|0000|020|0|01032018|").endsWith("|\r\n");
	}

	@Test
	void acceptsADateRangeWithinTheMonth() throws Exception {
		List<String> lines = generate("\"startDate\": \"2018-03-10\", \"endDate\": \"2018-03-11\"");

		assertThat(lines.get(0)).startsWith("|0000|020|0|10032018|11032018|");
		assertThat(lines.stream().filter(line -> line.startsWith("|C100|"))).hasSize(2);
	}

	@Test
	void answersAnEmptyButCompleteFileForAPeriodWithoutDocuments() throws Exception {
		List<String> lines = generate("\"period\": \"2018-01\"");

		assertThat(lines.stream().filter(line -> line.startsWith("|C100|"))).isEmpty();
		assertThat(lines).contains("|C001|1|", "|E110|0,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|0,00|"
				+ "0,00|0,00|");
	}

	@Test
	void failsWithTheListOfMissingRecordsAndGeneratesNothing() throws Exception {
		mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON).content("""
				{"companyId": "%s", "period": "2018-03",
				 "taxpayer": {"legalName": "Empresa Teste Ltda", "profile": "A", "activity": "OTHER",
				 "zipCode": "01310100"},
				 "accountant": {"name": "Contador Teste", "cpf": "111.111.111-11"}}""".formatted(company.value())))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.message").value("SPED Fiscal not generated: 3 mandatory record(s) missing or invalid"))
				.andExpect(jsonPath("$.errors.length()").value(3))
				.andExpect(jsonPath("$.errors[0].record").value("0000"))
				.andExpect(jsonPath("$.errors[0].message").value("COD_MUN: the 7-digit IBGE municipality code is required"))
				.andExpect(jsonPath("$.errors[1].record").value("0100"))
				.andExpect(jsonPath("$.errors[1].message").value("CRC: is required"))
				.andExpect(jsonPath("$.errors[2].message").value("CPF: the accountant's CPF is missing or invalid"))
				.andExpect(jsonPath("$.txt").doesNotExist());
	}

	@Test
	void failsWhenThePeriodSpansTwoMonths() throws Exception {
		mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON)
				.content(request("\"startDate\": \"2018-03-20\", \"endDate\": \"2018-04-05\"")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.errors[0].message").value(org.hamcrest.Matchers.containsString("one calendar month")));
	}

	@Test
	void answersNotFoundForAnUnknownCompany() throws Exception {
		mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON)
				.content(request(UUID.randomUUID().toString(), "\"period\": \"2018-03\""))).andExpect(status().isNotFound());
	}

	@Test
	void answersBadRequestWhenThePeriodIsMissingAmbiguousOrMalformed() throws Exception {
		for (String period : List.of("", "\"period\": \"March\"", "\"startDate\": \"2018-03-01\"",
				"\"period\": \"2018-03\", \"startDate\": \"2018-03-01\", \"endDate\": \"2018-03-05\"",
				"\"startDate\": \"2018-03-05\", \"endDate\": \"2018-03-01\"")) {
			mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON).content(request(period)))
					.andExpect(status().isBadRequest());
		}
		mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON)
				.content("{\"period\": \"2018-03\", " + TAXPAYER + ", " + ACCOUNTANT + "}"))
				.andExpect(status().isBadRequest());
	}

	private List<String> generate(String period) throws Exception {
		String body = mockMvc.perform(post("/api/sped/fiscal").contentType(MediaType.APPLICATION_JSON)
				.content(request(period))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String txt = new String(Base64.getDecoder().decode((String) JsonPath.read(body, "$.txt")),
				StandardCharsets.ISO_8859_1);
		return List.of(txt.split("\r\n"));
	}

	private String request(String period) {
		return request(company.value().toString(), period);
	}

	private String request(String companyId, String period) {
		return "{\"companyId\": \"" + companyId + "\"" + (period.isEmpty() ? "" : ", " + period) + ", " + TAXPAYER
				+ ", " + ACCOUNTANT + "}";
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2018, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private void saveCompany(CompanyId id, String cnpj) {
		companyRepositoryPort.save(Company.of(id, Document.cnpj(cnpj), "123456789", "987654", "6201500",
				TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null));
	}

	private void inbound(CompanyId owner, String number, Instant issuedAt, String supplier, String total,
			String icms, boolean confirmed) {
		var received = LivrosFiscaisFixtures.receivedNfe(owner, "1", number, supplier, "1102", "1102", total, icms,
				"0", "0", "0", issuedAt);
		inboundNfeRepositoryPort.save(confirmed ? SpedFiscalFixtures.confirmed(received) : received);
	}

	private NfeDocument nfe(CompanyId issuer, NfeDocumentStatus status, String cfop, long number,
			Instant authorizedAt) {
		return LivrosFiscaisFixtures.issuedNfe(issuer, status, cfop, "1", number, authorizedAt);
	}

	private void voided(CompanyId owner, String series, long start, long end, Instant voidedAt) {
		voidedNumberRangeRepositoryPort.save(VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), owner,
				FiscalDocumentType.NFE, series, start, end, "formulários danificados", "void-protocol", voidedAt));
	}
}
