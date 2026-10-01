package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.tax.SpedContribuicoesFixtures;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

/**
 * Runs the real controller, service, repositories and file writer together: the EFD Contribuições of one company's
 * month is built from what {@code tax} stores, nothing of another company or another month leaks into it, and the file
 * is internally consistent (every block closer and every Block 9 counter matches the lines actually written).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GenerateSpedContribuicoesEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;

	@Autowired
	private NfeRepositoryPort nfeRepositoryPort;

	@Autowired
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	private final CompanyId company = CompanyId.of(UUID.randomUUID());
	private final CompanyId otherCompany = CompanyId.of(UUID.randomUUID());
	private final CompanyId simples = CompanyId.of(UUID.randomUUID());
	private NfeDocument authorizedNfe;

	@BeforeEach
	void seed() {
		saveCompany(company, "11.222.333/0001-81", TaxRegime.LUCRO_REAL);
		saveCompany(otherCompany, "11.444.777/0001-61", TaxRegime.LUCRO_REAL);
		saveCompany(simples, "45.723.174/0001-10", TaxRegime.SIMPLES_NACIONAL);

		// March 2018, a month long past so other tests' documents cannot collide; the edges are inside it.
		authorizedNfe = SpedContribuicoesFixtures.issuedNfe(company, "5102", "1", 20L, at(3, 10, 12, 0), "1.65",
				"7.60");
		nfeRepositoryPort.save(authorizedNfe);
		nfeRepositoryPort.save(SpedContribuicoesFixtures.issuedNfe(company, "5102", "1", 21L, at(3, 31, 23, 30),
				"1.65", "7.60"));
		nfeRepositoryPort.save(SpedContribuicoesFixtures.issuedNfe(company, "5102", "1", 22L, at(3, 11, 12, 0),
				"1.65", "7.60").cancel("Cancelada a pedido", at(3, 12, 12, 0)));
		nfeRepositoryPort.save(SpedContribuicoesFixtures.issuedNfe(company, "5102", "1", 23L, at(4, 1, 0, 0),
				"1.65", "7.60"));
		nfeRepositoryPort.save(SpedContribuicoesFixtures.issuedNfe(otherCompany, "5102", "1", 24L, at(3, 10, 12, 0),
				"1.65", "7.60"));

		inbound(company, InboundNfeStatus.CONFIRMED, "100", "Fornecedor Alfa", "1102", "100.00", "1.65", "7.60",
				at(3, 1, 0, 0));
		inbound(company, InboundNfeStatus.PENDING_CONFERENCE, "101", "Fornecedor Pendente", "1102", "999.00",
				"1.65", "7.60", at(3, 5, 9, 0));
		inbound(company, InboundNfeStatus.CONFIRMED, "102", "Fornecedor Fevereiro", "1102", "999.00", "1.65", "7.60",
				at(2, 28, 23, 30));
		inbound(company, InboundNfeStatus.CONFIRMED, "103", "Fornecedor Abril", "1102", "999.00", "1.65", "7.60",
				at(4, 1, 0, 0));
		inbound(otherCompany, InboundNfeStatus.CONFIRMED, "104", "Fornecedor Alheio", "1102", "999.00", "1.65",
				"7.60", at(3, 5, 9, 0));
	}

	@Test
	@DisplayName("Assesses the period under the company's regime from its authorized and confirmed documents")
	void assessesThePeriodUnderTheCompanysRegimeOverItsAuthorizedAndConfirmedDocuments() throws Exception {
		mockMvc.perform(generate(company, "2018-03"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.companyId").value(company.value().toString()))
				.andExpect(jsonPath("$.period").value("2018-03"))
				.andExpect(jsonPath("$.fileName").value("EFD-Contribuicoes-11222333000181-201803.txt"))
				.andExpect(jsonPath("$.assessment.taxRegime").value("LUCRO_REAL"))
				.andExpect(jsonPath("$.assessment.incidence").value("NON_CUMULATIVE"))
				.andExpect(jsonPath("$.assessment.exitDocuments").value(2))
				.andExpect(jsonPath("$.assessment.entryDocuments").value(1))
				.andExpect(jsonPath("$.assessment.pis.revenue").value(200.0))
				.andExpect(jsonPath("$.assessment.pis.contribution").value(3.3))
				.andExpect(jsonPath("$.assessment.pis.credit").value(1.65))
				.andExpect(jsonPath("$.assessment.pis.payable").value(1.65))
				.andExpect(jsonPath("$.assessment.cofins.contribution").value(15.2))
				.andExpect(jsonPath("$.assessment.cofins.credit").value(7.6))
				.andExpect(jsonPath("$.assessment.cofins.payable").value(7.6));
	}

	@Test
	@DisplayName("Writes an EFD whose blocks and block 9 counters match the lines actually written")
	void writesAnEfdWhoseBlocksAndBlockNineCountersMatchTheLinesActuallyWritten() throws Exception {
		List<String> lines = txt(company, "2018-03");

		assertStructureIsConsistent(lines);
		assertThat(lines.getFirst()).isEqualTo("|0000|006|0|||01032018|31032018||11222333000181|SP|||00|2|");
		assertThat(lines.get(1)).isEqualTo("|0001|0|");
		assertThat(lines.getLast()).isEqualTo("|9999|" + lines.size() + "|");
	}

	@Test
	@DisplayName("Writes the company's documents of the period and the contributions they assess")
	void writesTheCompanysDocumentsOfThePeriodAndTheContributionsTheyAssess() throws Exception {
		List<String> lines = txt(company, "2018-03");

		assertThat(lines).contains("|0110|1|1|1||", "|C010|11222333000181|2|", "|M001|0|");
		assertThat(lines.stream().filter(line -> line.startsWith("|C100|"))).hasSize(3);
		assertThat(lines.stream().filter(line -> line.startsWith("|C170|"))).hasSize(3);
		assertThat(lines).anyMatch(line -> line.startsWith("|C100|1|0|11444777000161|55|00|1|20|"
				+ authorizedNfe.getAccessKey() + "|10032018|10032018|100,00|"));
		assertThat(lines).contains("|M200|3,30|1,65|0,00|1,65|0,00|0,00|1,65|0,00|0,00|0,00|0,00|1,65|",
				"|M600|15,20|7,60|0,00|7,60|0,00|0,00|7,60|0,00|0,00|0,00|0,00|7,60|");
		assertThat(lines.stream().filter(line -> line.startsWith("|M100|"))).containsExactly(
				"|M100|101|0|100,00|1,6500|||1,65|0,00|0,00|0,00|1,65|0|1,65|0,00|");
	}

	@Test
	@DisplayName("Leaves out documents from other periods, other companies, and those not confirmed or not authorized")
	void leavesOutTheDocumentsOfOtherPeriodsOtherCompaniesAndThoseNotConfirmedOrNotAuthorized() throws Exception {
		String txt = String.join("\n", txt(company, "2018-03"));

		assertThat(txt).doesNotContain("Fornecedor Pendente", "Fornecedor Fevereiro", "Fornecedor Abril",
				"Fornecedor Alheio", "999,00", String.format("%044d", 22L), String.format("%044d", 23L),
				String.format("%044d", 24L));
		assertThat(txt).contains(String.format("%044d", 20L), String.format("%044d", 21L), "Fornecedor Alfa");
	}

	@Test
	@DisplayName("Writes an empty but well-formed file for a period without documents")
	void writesAnEmptyButWellFormedFileForAPeriodWithoutDocuments() throws Exception {
		List<String> lines = txt(company, "2018-01");

		assertStructureIsConsistent(lines);
		assertThat(lines).contains("|0000|006|0|||01012018|31012018||11222333000181|SP|||00|2|", "|C001|1|", "|M001|1|")
				.noneMatch(line -> line.startsWith("|C100|") || line.startsWith("|M200|"));
	}

	@Test
	@DisplayName("Refuses a Simples Nacional company with a 409 conflict")
	void refusesACompanyUnderTheSimplesNacionalWithAConflict() throws Exception {
		mockMvc.perform(generate(simples, "2018-03")).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Returns 404 for an unknown company")
	void answersNotFoundForAnUnknownCompany() throws Exception {
		mockMvc.perform(post("/api/sped/contribuicoes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"companyId\":\"" + UUID.randomUUID() + "\",\"period\":\"2018-03\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Returns 400 when the company or period is missing or malformed")
	void answersBadRequestWhenTheCompanyOrThePeriodIsMissingOrMalformed() throws Exception {
		String companyId = company.value().toString();
		for (String body : List.of("{\"period\":\"2018-03\"}", "{\"companyId\":\"" + companyId + "\"}",
				"{\"companyId\":\"" + companyId + "\",\"period\":\"March\"}",
				"{\"companyId\":\"not-a-uuid\",\"period\":\"2018-03\"}", "")) {
			mockMvc.perform(post("/api/sped/contribuicoes").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest());
		}
	}

	/** The file's lines as the validator reads them: ISO-8859-1, CR LF. */
	private List<String> txt(CompanyId owner, String period) throws Exception {
		String body = mockMvc.perform(generate(owner, period)).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		byte[] bytes = Base64.getDecoder().decode((String) JsonPath.read(body, "$.txt"));
		String text = new String(bytes, StandardCharsets.ISO_8859_1);
		assertThat(text).endsWith("\r\n");
		return Arrays.asList(text.split("\r\n"));
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder generate(
			CompanyId owner, String period) {
		return post("/api/sped/contribuicoes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"companyId\":\"" + owner.value() + "\",\"period\":\"" + period + "\"}");
	}

	/**
	 * What the SPED validator checks of a file's skeleton: every line is {@code |REG|...|}; the file opens with
	 * {@code 0000} and {@code 0001}; each block's {@code X990} counts its own lines; Block 9's {@code 9900} lists every
	 * register with the number of times it was written; {@code 9990} and {@code 9999} count Block 9 and the file.
	 */
	private static void assertStructureIsConsistent(List<String> lines) {
		assertThat(lines).allSatisfy(line -> assertThat(line).matches("\\|[0-9A-Z]{4}(\\|[^|]*)*\\|"));
		assertThat(lines.get(0)).startsWith("|0000|");
		assertThat(lines.get(1)).startsWith("|0001|");

		Map<String, Integer> written = new TreeMap<>();
		Map<Character, Integer> perBlock = new TreeMap<>();
		lines.forEach(line -> {
			written.merge(register(line), 1, Integer::sum);
			perBlock.merge(register(line).charAt(0), 1, Integer::sum);
		});
		for (Map.Entry<Character, Integer> block : perBlock.entrySet()) {
			String closer = block.getKey() + "990";
			assertThat(written).as("closer of block " + block.getKey()).containsKey(closer);
			assertThat(field(lines, closer, 0)).as(closer).isEqualTo(String.valueOf(block.getValue()));
		}
		Map<String, Integer> declared = new TreeMap<>();
		lines.stream().filter(line -> line.startsWith("|9900|")).forEach(
				line -> declared.put(line.split("\\|")[2], Integer.parseInt(line.split("\\|")[3])));
		assertThat(declared).isEqualTo(written);
		assertThat(field(lines, "9999", 0)).isEqualTo(String.valueOf(lines.size()));
		assertThat(lines.stream().filter(line -> line.startsWith("|0") || line.startsWith("|A") || line.startsWith("|C")
				|| line.startsWith("|D") || line.startsWith("|F") || line.startsWith("|I") || line.startsWith("|M")
				|| line.startsWith("|P") || line.startsWith("|1") || line.startsWith("|9"))).hasSize(lines.size());
		assertThat(Arrays.asList(lines.stream().map(line -> register(line).charAt(0)).distinct().toArray()))
				.containsExactly('0', 'A', 'C', 'D', 'F', 'I', 'M', 'P', '1', '9');
	}

	private static String register(String line) {
		return line.split("\\|")[1];
	}

	private static String field(List<String> lines, String register, int index) {
		return lines.stream().filter(line -> register(line).equals(register)).findFirst().orElseThrow()
				.split("\\|")[2 + index];
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2018, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private void saveCompany(CompanyId id, String cnpj, TaxRegime regime) {
		companyRepositoryPort.save(Company.of(id, Document.cnpj(cnpj), "123456789", "987654", "4712100", regime,
				regime == TaxRegime.SIMPLES_NACIONAL, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null));
	}

	private void inbound(CompanyId owner, InboundNfeStatus status, String number, String supplier, String cfop,
			String value, String pis, String cofins, Instant issuedAt) {
		inboundNfeRepositoryPort.save(SpedContribuicoesFixtures.receivedNfe(owner, status, number,
				"11.222.333/0001-81", supplier, cfop, value, pis, cofins, issuedAt));
	}
}
