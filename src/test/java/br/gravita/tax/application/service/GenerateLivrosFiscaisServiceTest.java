package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.inbound.tax.GenerateLivrosFiscaisCommand;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Flow;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisReport;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort.FiscalBookFiles;
import br.gravita.core.usercases.tax.GenerateLivrosFiscaisService;
import br.gravita.tax.LivrosFiscaisFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerateLivrosFiscaisServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");
	private static final byte[] PDF = {1, 2, 3};
	private static final byte[] TXT = {4, 5};

	private final CompanyRepositoryPort companies = mock(CompanyRepositoryPort.class);
	private final NfeRepositoryPort nfes = mock(NfeRepositoryPort.class);
	private final InboundNfeRepositoryPort inbound = mock(InboundNfeRepositoryPort.class);
	private final VoidedNumberRangeRepositoryPort voided = mock(VoidedNumberRangeRepositoryPort.class);
	private final GenerateFiscalBookPort books = mock(GenerateFiscalBookPort.class);

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private GenerateLivrosFiscaisService service;

	@BeforeEach
	void setUp() {
		service = new GenerateLivrosFiscaisService(companies, nfes, inbound, voided, books,
				Clock.system(ZoneOffset.UTC));
		when(companies.findById(companyId)).thenReturn(Optional.of(Company.of(companyId, "Acme Ltda",
				Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500", TaxRegime.LUCRO_PRESUMIDO,
				false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "11999999999", null,
				null)));
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of());
		when(books.generate(any())).thenReturn(new FiscalBookFiles(PDF, TXT));
	}

	@Test
	@DisplayName("Reports not found for an unknown company without reading or rendering anything")
	void answersNotFoundForAnUnknownCompanyWithoutReadingOrRenderingAnything() {
		CompanyId stranger = CompanyId.of(UUID.randomUUID());
		when(companies.findById(stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GenerateLivrosFiscaisCommand(stranger, PERIOD)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(nfes, inbound, voided, books);
	}

	@Test
	@DisplayName("Reads the company's documents over the whole month in the clock's time zone")
	void readsTheCompanysDocumentsOverTheWholeMonthInTheClocksZone() {
		service.execute(new GenerateLivrosFiscaisCommand(companyId, PERIOD));

		verify(nfes).findAuthorizedByCompanyBetween(companyId, FROM, TO);
		verify(inbound).findIssuedByCompanyBetween(companyId, FROM, TO);
		verify(voided).findByCompanyIdAndVoidedAtBetween(companyId, FROM, TO);

		GenerateLivrosFiscaisService saoPaulo = new GenerateLivrosFiscaisService(companies, nfes, inbound, voided,
				books, Clock.system(java.time.ZoneId.of("America/Sao_Paulo")));
		saoPaulo.execute(new GenerateLivrosFiscaisCommand(companyId, PERIOD));
		verify(nfes).findAuthorizedByCompanyBetween(companyId, Instant.parse("2028-02-01T03:00:00Z"),
				Instant.parse("2028-03-01T03:00:00Z"));
	}

	@Test
	@DisplayName("Books received NF-e as entries in document order with their CFOPs joined")
	void booksTheReceivedNfeAsEntriesInDocumentOrderWithTheirCfopsJoined() {
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "10", "Beta Ltda", "1102", "1403", "300.00",
						"36.00", "0", "0", "0", Instant.parse("2028-02-20T12:00:00Z")),
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "9", "Alfa SA", "1102", "1102", "100.00", "12.00",
						"4.00", "0.65", "3.00", Instant.parse("2028-02-20T08:00:00Z")),
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "5", "Gama ME", "2102", "2102", "50.00", "0", "0",
						"0", "0", Instant.parse("2028-02-05T08:00:00Z"))));

		LivrosFiscaisBooks result = execute().books();

		assertThat(result.entryBook().lines()).extracting(LivrosFiscaisBooks.Line::counterpartName)
				.containsExactly("Gama ME", "Alfa SA", "Beta Ltda");
		assertThat(result.entryBook().lines()).extracting(LivrosFiscaisBooks.Line::cfop)
				.containsExactly("2102", "1102", "1102/1403");
		assertThat(result.entryBook().lines()).allMatch(line -> line.flow() == Flow.ENTRY);
		assertThat(result.entryBook().totalValue()).isEqualByComparingTo("450.00");
		assertThat(result.exitBook().lines()).isEmpty();
		assertThat(result.exitBook().totalValue()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Books issued NF-e by CFOP as entry or exit and by authorization day")
	void booksTheIssuedNfeByTheirCfopEntryOrExitAndByTheirAuthorizationDay() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 12L,
						Instant.parse("2028-02-29T23:30:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "6102", "1", 9L,
						Instant.parse("2028-02-29T23:30:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "1202", "1", 11L,
						Instant.parse("2028-02-10T10:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "3102", "1", 13L,
						Instant.parse("2028-02-11T10:00:00Z"))));

		LivrosFiscaisBooks result = execute().books();

		assertThat(result.exitBook().lines()).extracting(LivrosFiscaisBooks.Line::number)
				.containsExactly("9", "12");
		assertThat(result.exitBook().lines()).extracting(LivrosFiscaisBooks.Line::cfop)
				.containsExactly("6102", "5102");
		assertThat(result.exitBook().lines().get(0).date()).hasToString("2028-02-29");
		assertThat(result.exitBook().lines()).allMatch(line -> line.flow() == Flow.EXIT);
		assertThat(result.entryBook().lines()).extracting(LivrosFiscaisBooks.Line::number)
				.containsExactly("11", "13");
		LivrosFiscaisBooks.Line exit = result.exitBook().lines().get(0);
		assertThat(exit.counterpartName()).isEqualTo("Cliente SA");
		assertThat(exit.totalValue()).isEqualByComparingTo("1015.00");
		assertThat(exit.icmsValue()).isEqualByComparingTo("18.00");
		assertThat(exit.ipiValue()).isEqualByComparingTo("5.00");
		assertThat(exit.pisValue()).isEqualByComparingTo("1.65");
		assertThat(exit.cofinsValue()).isEqualByComparingTo("7.60");
	}

	@Test
	@DisplayName("Carries voided ranges on the exit book so numbering gaps are explained")
	void carriesTheVoidedRangesOnTheExitBookSoTheGapsInItsNumberingAreExplained() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 100L,
						Instant.parse("2028-02-10T10:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 111L,
						Instant.parse("2028-02-11T10:00:00Z"))));
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of(
				voidedRange("1", 101L, 110L, "numbers printed on damaged forms", "2028-02-10T15:00:00Z"),
				voidedRange("2", 7L, 7L, "skipped by the printer", "2028-02-12T09:00:00Z")));

		LivrosFiscaisBooks result = execute().books();

		assertThat(result.exitBook().voidedRanges()).extracting(LivrosFiscaisBooks.VoidedRange::series,
				LivrosFiscaisBooks.VoidedRange::startNumber, LivrosFiscaisBooks.VoidedRange::endNumber,
				LivrosFiscaisBooks.VoidedRange::quantity, LivrosFiscaisBooks.VoidedRange::justification,
				LivrosFiscaisBooks.VoidedRange::sefazProtocol)
				.containsExactly(
						org.assertj.core.api.Assertions.tuple("1", 101L, 110L, 10L, "numbers printed on damaged forms",
								"void-protocol"),
						org.assertj.core.api.Assertions.tuple("2", 7L, 7L, 1L, "skipped by the printer",
								"void-protocol"));
		assertThat(result.entryBook().voidedRanges()).isEmpty();
	}

	@Test
	@DisplayName("Leaves out voided ranges of other document types")
	void leavesOutVoidedRangesOfOtherDocumentTypes() {
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of(
				VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), companyId, FiscalDocumentType.NFSE, "1",
						1L, 2L, "justification", "protocol", Instant.parse("2028-02-12T09:00:00Z"))));

		assertThat(execute().books().exitBook().voidedRanges()).isEmpty();
	}

	@Test
	@DisplayName("Assesses ICMS with exits as debits and entries as credits")
	void assessesIcmsWithTheExitsAsDebitsAndTheEntriesAsCredits() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 1L,
						Instant.parse("2028-02-10T10:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 2L,
						Instant.parse("2028-02-11T10:00:00Z"))));
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "10", "Beta Ltda", "1102", "1102", "300.00",
						"54.00", "0", "0", "0", Instant.parse("2028-02-20T12:00:00Z")),
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "11", "Isenta ME", "1102", "1102", "30.00", "0",
						"0", "0", "0", Instant.parse("2028-02-21T12:00:00Z"))));

		LivrosFiscaisBooks.IcmsAssessment assessment = execute().books().icmsAssessmentBook();

		assertThat(assessment.debit()).isEqualByComparingTo("36.00");
		assertThat(assessment.credit()).isEqualByComparingTo("54.00");
		assertThat(assessment.balance()).isEqualByComparingTo("-18.00");
		assertThat(assessment.lines()).extracting(LivrosFiscaisBooks.Line::flow)
				.containsExactly(Flow.EXIT, Flow.EXIT, Flow.ENTRY);
		assertThat(assessment.lines()).extracting(LivrosFiscaisBooks.Line::counterpartName)
				.doesNotContain("Isenta ME");
	}

	@Test
	@DisplayName("Summarises ICMS, IPI, PIS and COFINS of the exits and the entries")
	void summarisesIcmsIpiPisAndCofinsOfTheExitsAndTheEntries() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 1L,
						Instant.parse("2028-02-10T10:00:00Z"))));
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.receivedNfe(companyId, "1", "10", "Beta Ltda", "1102", "1102", "300.00",
						"12.00", "2.00", "0.65", "3.00", Instant.parse("2028-02-20T12:00:00Z"))));

		LivrosFiscaisBooks.TaxSummary summary = execute().books().taxSummary();

		assertTotals(summary.icms(), "18.00", "12.00", "6.00");
		assertTotals(summary.ipi(), "5.00", "2.00", "3.00");
		assertTotals(summary.pis(), "1.65", "0.65", "1.00");
		assertTotals(summary.cofins(), "7.60", "3.00", "4.60");
	}

	@Test
	@DisplayName("Returns empty books and zero totals for a period without documents")
	void answersEmptyBooksAndZeroTotalsForAPeriodWithoutDocuments() {
		LivrosFiscaisBooks result = execute().books();

		assertThat(result.entryBook().lines()).isEmpty();
		assertThat(result.exitBook().lines()).isEmpty();
		assertThat(result.icmsAssessmentBook().lines()).isEmpty();
		assertThat(result.icmsAssessmentBook().balance()).isEqualByComparingTo("0");
		assertTotals(result.taxSummary().icms(), "0", "0", "0");
		assertTotals(result.taxSummary().cofins(), "0", "0", "0");
	}

	@Test
	@DisplayName("Identifies the company and period and hands the books to the renderer")
	void identifiesTheCompanyAndThePeriodAndHandsTheBooksToTheRenderer() {
		LivrosFiscaisReport report = execute();

		ArgumentCaptor<LivrosFiscaisBooks> rendered = ArgumentCaptor.forClass(LivrosFiscaisBooks.class);
		verify(books).generate(rendered.capture());
		assertThat(rendered.getValue()).isSameAs(report.books());
		assertThat(report.books().companyId()).isEqualTo(companyId);
		assertThat(report.books().companyCnpj()).isEqualTo("11222333000181");
		assertThat(report.books().companyIe()).isEqualTo("123456789");
		assertThat(report.books().period()).isEqualTo(PERIOD);
		assertThat(report.pdf()).isEqualTo(PDF);
		assertThat(report.txt()).isEqualTo(TXT);
	}

	private LivrosFiscaisReport execute() {
		return service.execute(new GenerateLivrosFiscaisCommand(companyId, PERIOD));
	}

	private VoidedNumberRange voidedRange(String series, long start, long end, String justification, String at) {
		return VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), companyId, FiscalDocumentType.NFE,
				series, start, end, justification, "void-protocol", Instant.parse(at));
	}

	private static void assertTotals(LivrosFiscaisBooks.Totals totals, String onExits, String onEntries,
			String balance) {
		assertThat(totals.onExits()).isEqualByComparingTo(onExits);
		assertThat(totals.onEntries()).isEqualByComparingTo(onEntries);
		assertThat(totals.balance()).isEqualByComparingTo(balance);
	}
}
