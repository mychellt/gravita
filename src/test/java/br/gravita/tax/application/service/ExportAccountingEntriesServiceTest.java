package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.inbound.tax.AccountingEntry;
import br.gravita.core.ports.inbound.tax.AccountingEntry.Flow;
import br.gravita.core.ports.inbound.tax.AccountingExportFile;
import br.gravita.core.ports.inbound.tax.AccountingExportFormat;
import br.gravita.core.ports.inbound.tax.ExportAccountingEntriesCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.ExportAccountingFilePort;
import br.gravita.core.usercases.tax.ExportAccountingEntriesService;
import br.gravita.tax.LivrosFiscaisFixtures;
import br.gravita.tax.SpedFiscalFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExportAccountingEntriesServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");
	private static final byte[] FILE = {1, 2, 3};

	private final CompanyRepositoryPort companies = mock(CompanyRepositoryPort.class);
	private final NfeRepositoryPort nfes = mock(NfeRepositoryPort.class);
	private final InboundNfeRepositoryPort inbound = mock(InboundNfeRepositoryPort.class);
	private final ExportAccountingFilePort files = mock(ExportAccountingFilePort.class);

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private ExportAccountingEntriesService service;

	@BeforeEach
	void setUp() {
		service = new ExportAccountingEntriesService(companies, nfes, inbound, files, Clock.system(ZoneOffset.UTC));
		when(companies.findById(companyId)).thenReturn(Optional.of(Company.of(companyId,
				Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500", TaxRegime.LUCRO_PRESUMIDO,
				false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "11999999999", null,
				null)));
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(files.export(any(), any())).thenReturn(FILE);
	}

	@Test
	@DisplayName("Reports not found for an unknown company without reading or exporting anything")
	void answersNotFoundForAnUnknownCompanyWithoutReadingOrExportingAnything() {
		CompanyId stranger = CompanyId.of(UUID.randomUUID());
		when(companies.findById(stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service
				.execute(new ExportAccountingEntriesCommand(stranger, PERIOD, AccountingExportFormat.CSV)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(nfes, inbound, files);
	}

	@Test
	@DisplayName("Reads authorized and confirmed documents over the whole month in the clock's time zone")
	void readsTheAuthorizedAndTheConfirmedDocumentsOverTheWholeMonthInTheClocksZone() {
		execute(AccountingExportFormat.CSV);

		verify(nfes).findAuthorizedByCompanyBetween(companyId, FROM, TO);
		verify(inbound).findConfirmedByCompanyBetween(companyId, FROM, TO);

		new ExportAccountingEntriesService(companies, nfes, inbound, files,
				Clock.system(ZoneId.of("America/Sao_Paulo")))
				.execute(new ExportAccountingEntriesCommand(companyId, PERIOD, AccountingExportFormat.CSV));
		verify(nfes).findAuthorizedByCompanyBetween(companyId, Instant.parse("2028-02-01T03:00:00Z"),
				Instant.parse("2028-03-01T03:00:00Z"));
	}

	@Test
	@DisplayName("Turns every issued and received document into one entry, oldest first")
	void turnsEveryIssuedAndReceivedDocumentIntoOneEntryOldestFirst() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20,
						Instant.parse("2028-02-10T12:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "1949", "1", 21,
						Instant.parse("2028-02-03T12:00:00Z"))));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				SpedFiscalFixtures.confirmed(LivrosFiscaisFixtures.receivedNfe(companyId, "1", "9", "Alfa SA", "1102",
						"1403", "165.00", "27.00", "5.00", "1.00", "4.00", Instant.parse("2028-02-05T08:00:00Z")))));

		AccountingExportFile file = execute(AccountingExportFormat.CSV);

		List<AccountingEntry> entries = exported(AccountingExportFormat.CSV);
		assertThat(file.entryCount()).isEqualTo(3);
		assertThat(entries).extracting(AccountingEntry::date).containsExactly(LocalDate.of(2028, 2, 3),
				LocalDate.of(2028, 2, 5), LocalDate.of(2028, 2, 10));
		assertThat(entries).extracting(AccountingEntry::flow).containsExactly(Flow.ENTRY, Flow.ENTRY, Flow.EXIT);
		assertThat(entries).extracting(AccountingEntry::cfop).containsExactly("1949", "1102/1403", "5102");
	}

	@Test
	@DisplayName("Carries each document's counterpart and values into its entry")
	void carriesTheDocumentsCounterpartAndValues() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(LivrosFiscaisFixtures
				.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20,
						Instant.parse("2028-02-10T12:00:00Z"))));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				SpedFiscalFixtures.confirmed(LivrosFiscaisFixtures.receivedNfe(companyId, "2", "9", "Alfa SA", "1102",
						"1403", "165.00", "27.00", "5.00", "1.00", "4.00", Instant.parse("2028-02-05T08:00:00Z")))));

		execute(AccountingExportFormat.TXT);

		AccountingEntry received = exported(AccountingExportFormat.TXT).get(0);
		assertThat(received.series()).isEqualTo("2");
		assertThat(received.number()).isEqualTo("9");
		assertThat(received.counterpartName()).isEqualTo("Alfa SA");
		assertThat(received.counterpartDocument()).isEqualTo("11222333000181");
		assertThat(received.totalValue()).isEqualByComparingTo("165.00");
		assertThat(received.icmsValue()).isEqualByComparingTo("27.00");
		assertThat(received.ipiValue()).isEqualByComparingTo("5.00");
		assertThat(received.pisValue()).isEqualByComparingTo("1.00");
		assertThat(received.cofinsValue()).isEqualByComparingTo("4.00");
		AccountingEntry issued = exported(AccountingExportFormat.TXT).get(1);
		assertThat(issued.number()).isEqualTo("20");
		assertThat(issued.counterpartName()).isEqualTo("Cliente SA");
		assertThat(issued.totalValue()).isEqualByComparingTo("1015.00");
		assertThat(issued.icmsValue()).isEqualByComparingTo("18.00");
		assertThat(issued.cofinsValue()).isEqualByComparingTo("7.60");
	}

	@Test
	@DisplayName("Exports an empty period as a file without entries")
	void exportsAnEmptyPeriodAsAFileWithoutEntries() {
		AccountingExportFile file = execute(AccountingExportFormat.CSV);

		assertThat(file.entryCount()).isZero();
		assertThat(file.content()).isEqualTo(FILE);
		assertThat(exported(AccountingExportFormat.CSV)).isEmpty();
	}

	@Test
	@DisplayName("Names the file after the company, the period and the format")
	void namesTheFileAfterTheCompanyThePeriodAndTheFormat() {
		assertThat(execute(AccountingExportFormat.CSV).fileName())
				.isEqualTo("accounting-entries-11222333000181-2028-02.csv");
		AccountingExportFile txt = execute(AccountingExportFormat.TXT);
		assertThat(txt.fileName()).isEqualTo("accounting-entries-11222333000181-2028-02.txt");
		assertThat(txt.contentType()).isEqualTo("text/plain");
	}

	@Test
	@DisplayName("Exports as CSV when no format is given")
	void exportsAsCsvWhenNoFormatIsGiven() {
		AccountingExportFile file = service.execute(new ExportAccountingEntriesCommand(companyId, PERIOD, null));

		assertThat(file.format()).isEqualTo(AccountingExportFormat.CSV);
		verify(files).export(any(), eq(AccountingExportFormat.CSV));
	}

	private AccountingExportFile execute(AccountingExportFormat format) {
		return service.execute(new ExportAccountingEntriesCommand(companyId, PERIOD, format));
	}

	@SuppressWarnings("unchecked")
	private List<AccountingEntry> exported(AccountingExportFormat format) {
		ArgumentCaptor<List<AccountingEntry>> captor = ArgumentCaptor.forClass(List.class);
		verify(files).export(captor.capture(), eq(format));
		return captor.getValue();
	}
}
