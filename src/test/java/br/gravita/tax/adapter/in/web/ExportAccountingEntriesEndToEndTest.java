package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.tax.LivrosFiscaisFixtures;
import br.gravita.tax.SpedFiscalFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 * Runs the real controller, service, repositories and file adapter together: the export of one company's month holds
 * the NFe it issued and SEFAZ authorized and the NFe it received and confirmed, and nothing else.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ExportAccountingEntriesEndToEndTest {

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
	private NfeDocument authorizedNfe;

	@BeforeEach
	void seed() {
		saveCompany(company, "11.222.333/0001-81");
		saveCompany(otherCompany, "11.444.777/0001-61");

		// Inside March 2018, edges included; only the confirmed receipts are exported.
		inbound(company, "100", at(3, 1, 0, 0), "Fornecedor Alfa", true);
		inbound(company, "101", at(3, 31, 23, 30), "Fornecedor Beta", true);
		inbound(company, "102", at(3, 15, 10, 0), "Fornecedor Pendente", false);
		inbound(company, "103", at(2, 28, 23, 30), "Fornecedor Fevereiro", true);
		inbound(company, "104", at(4, 1, 0, 0), "Fornecedor Abril", true);
		inbound(otherCompany, "105", at(3, 5, 9, 0), "Fornecedor Alheio", true);

		authorizedNfe = nfe(company, NfeDocumentStatus.AUTHORIZED, 20L, at(3, 10, 12, 0));
		nfeRepositoryPort.save(authorizedNfe);
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.REJECTED, 21L, null));
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.CANCELLED, 22L, at(3, 11, 12, 0)));
		nfeRepositoryPort.save(nfe(company, NfeDocumentStatus.AUTHORIZED, 23L, at(4, 1, 12, 0)));
		nfeRepositoryPort.save(nfe(otherCompany, NfeDocumentStatus.AUTHORIZED, 24L, at(3, 10, 12, 0)));
	}

	@Test
	@DisplayName("Exports every authorized and confirmed document of the period as CSV")
	void exportsEveryAuthorizedAndConfirmedDocumentOfThePeriodAsCsv() throws Exception {
		var result = mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON)
				.content(body(company, "2018-03", "CSV"))).andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"accounting-entries-11222333000181-2018-03.csv\""))
				.andExpect(header().string("X-Entry-Count", "3")).andReturn();

		String[] lines = result.getResponse().getContentAsString(StandardCharsets.UTF_8).split("\r\n");
		assertThat(lines).hasSize(4);
		assertThat(lines[0]).startsWith("Data;Natureza;Série;Número;Chave de acesso");
		assertThat(lines[1]).startsWith("01/03/2018;ENTRADA;1;100;").contains("Fornecedor Alfa");
		assertThat(lines[2]).startsWith("10/03/2018;SAIDA;1;20;" + authorizedNfe.getAccessKey() + ";Cliente SA;")
				.contains(";5102;1015,00;18,00;5,00;1,65;7,60");
		assertThat(lines[3]).startsWith("31/03/2018;ENTRADA;1;101;").contains("Fornecedor Beta");
		assertThat(String.join("\n", lines)).doesNotContain("Pendente", "Fevereiro", "Abril", "Alheio");
	}

	@Test
	@DisplayName("Exports the same accounting entries in TXT format")
	void exportsTheSameEntriesAsTxt() throws Exception {
		var result = mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON)
				.content(body(company, "2018-03", "TXT"))).andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"accounting-entries-11222333000181-2018-03.txt\""))
				.andExpect(header().string("X-Entry-Count", "3")).andReturn();

		String[] lines = result.getResponse().getContentAsString(StandardCharsets.UTF_8).split("\r\n");
		assertThat(lines).hasSize(3);
		assertThat(lines[0]).startsWith("01/03/2018 ENTRADA").contains("Fornecedor Alfa");
		assertThat(lines[1]).startsWith("10/03/2018 SAIDA").contains(authorizedNfe.getAccessKey(), "Cliente SA");
		assertThat(lines[2]).startsWith("31/03/2018 ENTRADA").contains("Fornecedor Beta");
	}

	@Test
	@DisplayName("Defaults to CSV when no format is requested")
	void exportsCsvWhenNoFormatIsGiven() throws Exception {
		mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON)
				.content("{\"companyId\":\"" + company.value() + "\",\"period\":\"2018-03\"}"))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
	}

	@Test
	@DisplayName("Returns an empty file for a period without documents")
	void answersAnEmptyFileForAPeriodWithoutDocuments() throws Exception {
		var result = mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON)
				.content(body(company, "2018-01", "CSV"))).andExpect(status().isOk())
				.andExpect(header().string("X-Entry-Count", "0")).andReturn();

		assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8).split("\r\n")).hasSize(1);
	}

	@Test
	@DisplayName("Returns 404 for an unknown company")
	void answersNotFoundForAnUnknownCompany() throws Exception {
		mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON)
				.content(body(CompanyId.of(UUID.randomUUID()), "2018-03", "CSV"))).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Returns 400 when the request is incomplete or malformed")
	void answersBadRequestWhenTheRequestIsIncompleteOrMalformed() throws Exception {
		String id = company.value().toString();
		for (String json : new String[] { "{\"period\":\"2018-03\"}", "{\"companyId\":\"" + id + "\"}",
				"{\"companyId\":\"" + id + "\",\"period\":\"March\"}",
				"{\"companyId\":\"" + id + "\",\"period\":\"2018-03\",\"format\":\"XML\"}",
				"{\"companyId\":\"not-a-uuid\",\"period\":\"2018-03\"}" }) {
			mockMvc.perform(post("/api/accounting/export").contentType(MediaType.APPLICATION_JSON).content(json))
					.andExpect(status().isBadRequest());
		}
	}

	private static String body(CompanyId id, String period, String format) {
		return "{\"companyId\":\"" + id.value() + "\",\"period\":\"" + period + "\",\"format\":\"" + format + "\"}";
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2018, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private void saveCompany(CompanyId id, String cnpj) {
		companyRepositoryPort.save(Company.of(id, Document.cnpj(cnpj), "123456789", "987654", "6201500",
				TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null));
	}

	private void inbound(CompanyId owner, String number, Instant issuedAt, String supplier, boolean confirmed) {
		var received = LivrosFiscaisFixtures.receivedNfe(owner, "1", number, supplier, "1102", "1403", "165.00",
				"27.00", "0", "0", "0", issuedAt);
		inboundNfeRepositoryPort.save(confirmed ? SpedFiscalFixtures.confirmed(received) : received);
	}

	private NfeDocument nfe(CompanyId issuer, NfeDocumentStatus status, long number, Instant authorizedAt) {
		return LivrosFiscaisFixtures.issuedNfe(issuer, status, "5102", "1", number, authorizedAt);
	}
}
