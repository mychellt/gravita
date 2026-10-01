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
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Accountant;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Finality;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Period;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Taxpayer;
import br.gravita.core.ports.inbound.tax.SpedFiscalFile;
import br.gravita.core.ports.inbound.tax.SpedValidationException;
import br.gravita.core.ports.inbound.tax.SpedValidationReport;
import br.gravita.core.ports.inbound.tax.SpedValidationReport.Issue;
import br.gravita.core.ports.inbound.tax.SpedValidationReport.Severity;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedLayout;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import br.gravita.core.usercases.tax.GenerateSpedFiscalService;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerateSpedFiscalServiceTest {

	private static final YearMonth MONTH = YearMonth.of(2028, 2);
	private static final Period PERIOD = Period.ofMonth(MONTH);
	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");
	private static final byte[] TXT = {1, 2, 3};

	private final CompanyRepositoryPort companies = mock(CompanyRepositoryPort.class);
	private final NfeRepositoryPort nfes = mock(NfeRepositoryPort.class);
	private final InboundNfeRepositoryPort inbound = mock(InboundNfeRepositoryPort.class);
	private final VoidedNumberRangeRepositoryPort voided = mock(VoidedNumberRangeRepositoryPort.class);
	private final GenerateSpedFilePort spedFile = mock(GenerateSpedFilePort.class);

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private GenerateSpedFiscalService service;

	@BeforeEach
	void setUp() {
		service = new GenerateSpedFiscalService(companies, nfes, inbound, voided, spedFile,
				Clock.system(ZoneOffset.UTC));
		when(companies.findById(companyId)).thenReturn(Optional.of(Company.of(companyId,
				Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500", TaxRegime.LUCRO_PRESUMIDO,
				false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "(11) 99999-9999",
				null, null)));
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of());
		when(spedFile.generate(any(SpedLayout.class))).thenReturn(TXT);
	}

	@Test
	void answersNotFoundForAnUnknownCompanyWithoutReadingOrWritingAnything() {
		CompanyId stranger = CompanyId.of(UUID.randomUUID());
		when(companies.findById(stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(SpedFiscalFixtures.command(stranger, PERIOD)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(nfes, inbound, voided, spedFile);
	}

	@Test
	void readsTheCompanysDocumentsOverEveryDayOfThePeriodInTheClocksZone() {
		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		verify(nfes).findAuthorizedOrCancelledByCompanyBetween(companyId, FROM, TO);
		verify(inbound).findConfirmedByCompanyBetween(companyId, FROM, TO);
		verify(voided).findByCompanyIdAndVoidedAtBetween(companyId, FROM, TO);

		new GenerateSpedFiscalService(companies, nfes, inbound, voided, spedFile,
				Clock.system(ZoneId.of("America/Sao_Paulo")))
				.execute(SpedFiscalFixtures.command(companyId, new Period(LocalDate.of(2028, 2, 10),
						LocalDate.of(2028, 2, 20))));
		verify(nfes).findAuthorizedOrCancelledByCompanyBetween(companyId, Instant.parse("2028-02-10T03:00:00Z"),
				Instant.parse("2028-02-21T03:00:00Z"));
	}

	@Test
	void returnsTheGeneratedFileNamedAfterTheCompanyAndMonth() {
		SpedFiscalFile file = service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(file.content()).isEqualTo(TXT);
		assertThat(file.fileName()).isEqualTo("SPED-EFD-ICMS-IPI-11222333000181-2028-02.txt");
		assertThat(file.report().hasErrors()).isFalse();
	}

	@Test
	void laysOutEveryBlockOfTheLayoutInOrderWithTheEmptyOnesEmpty() {
		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		List<SpedBlock> blocks = blocks();
		assertThat(blocks).extracting(SpedBlock::id).containsExactly('0', 'B', 'C', 'D', 'E', 'G', 'H', 'K', '1');
		assertThat(blocks).filteredOn(block -> "BDGHK".indexOf(block.id()) >= 0)
				.allMatch(block -> block.records().isEmpty());
		assertThat(block('C').records()).isEmpty();
	}

	@Test
	void identifiesTheCompanyItsAddressAndTheAccountant() {
		service.execute(new GenerateSpedFiscalCommand(companyId, PERIOD, Finality.SUBSTITUTE,
				SpedFiscalFixtures.taxpayer(), SpedFiscalFixtures.accountant()));

		SpedBlock zero = block('0');
		assertThat(layout().header().register()).isEqualTo("0000");
		assertThat(texts(layout().header())).containsExactly("020", "1", "01022028", "29022028",
				"Empresa Teste Ltda", "11222333000181", null, "SP", "123456789", "3550308", "987654", null, "A", "1");
		assertThat(zero.records()).extracting(SpedRecord::register).containsExactly("0005", "0100");
		assertThat(texts(zero.records().get(0))).containsExactly("Teste", "01310100", "Rua Teste, 100", "100", null,
				"Bela Vista", "11999999999", null, "nfe@example.com");
		assertThat(texts(zero.records().get(1))).containsExactly("Contador Teste", "52998224725", "SP-123456/O-0",
				null, null, null, null, null, null, null, null, "contador@example.com", null);
	}

	@Test
	void reportsAnAuthorizedExitAsARegularC100WithItsValues() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z"))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		SpedRecord c100 = block('C').records().get(0);
		assertThat(c100.register()).isEqualTo("C100");
		assertThat(texts(c100)).hasSize(28);
		assertThat(texts(c100)).containsExactly("1", "0", "11222333000181", "55", "00", "1", "20",
				texts(c100).get(7), "10022028", "10022028", "1015,00", "2", "0,00", "0,00", "1000,00", "9", "15,00",
				"0,00", "0,00", "100,00", "18,00", "0,00", "0,00", "5,00", "1,65", "7,60", "0,00", "0,00");
		assertThat(texts(c100).get(7)).matches("\\d{44}");
	}

	@Test
	void reportsAnNfeUnderAnEntryCfopAsAnEntryTheCompanyIssued() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "1202", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z"))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(texts(block('C').records().get(0))).startsWith("0", "0");
	}

	@Test
	void keepsACancelledNfeWithOnlyTheFieldsTheLayoutKeepsForIt() {
		var cancelled = LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.CANCELLED, "5102", "1", 21L,
				Instant.parse("2028-02-11T12:00:00Z"));
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(cancelled));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		SpedRecord c100 = block('C').records().get(0);
		assertThat(texts(c100)).containsExactly("1", "0", null, "55", "02", "1", "21", cancelled.getAccessKey());
		assertThat(block('0').records()).extracting(SpedRecord::register).doesNotContain("0150");
		assertThat(texts(block('E').records().get(1)).get(0)).isEqualTo("0,00");
	}

	@Test
	void reportsAConfirmedReceivedNfeAsAThirdPartyEntryReceivedWithinThePeriod() {
		InboundNfe received = SpedFiscalFixtures.confirmed(LivrosFiscaisFixtures.receivedNfe(companyId, "1", "10",
				"Alfa SA", "1102", "1102", "300.00", "36.00", "4.00", "0.65", "3.00",
				Instant.parse("2028-02-20T12:00:00Z")));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(received));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		SpedRecord c100 = block('C').records().get(0);
		assertThat(texts(c100)).containsExactly("0", "1", "11222333000181", "55", "00", "1", "10",
				received.getAccessKey(), "20022028", "20022028", "300,00", "2", "0,00", "0,00", "300,00", "9", "0,00",
				"0,00", "0,00", null, "36,00", "0,00", "0,00", "4,00", "0,65", "3,00", "0,00", "0,00");
		assertThat(texts(block('0').records().get(2))).containsExactly("11222333000181", "Alfa SA", "01058",
				"11222333000181", null, null, null, null, null, null, null, null);
	}

	@Test
	void neverDatesTheReceiptOfAGoodBeforeItsIssueOrAfterThePeriod() {
		InboundNfe late = SpedFiscalFixtures.confirmed(withImportedAt(Instant.parse("2028-04-02T10:00:00Z")));
		InboundNfe early = SpedFiscalFixtures.confirmed(withImportedAt(Instant.parse("2028-02-01T10:00:00Z")));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(late, early));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(block('C').records()).extracting(record -> texts(record).get(9))
				.containsExactlyInAnyOrder("29022028", "10022028");
	}

	private InboundNfe withImportedAt(Instant importedAt) {
		InboundNfe base = LivrosFiscaisFixtures.receivedNfe(companyId, "1", importedAt.toString().substring(5, 7)
				+ importedAt.toString().substring(8, 10), "Alfa SA", "1102", "1102", "10.00", "0", "0", "0", "0",
				Instant.parse("2028-02-10T12:00:00Z"));
		return InboundNfe.of(base.getId(), companyId, base.getAccessKey(), base.getSeries(), base.getNumber(),
				base.getSupplierDocument(), base.getSupplierName(), base.getIssuedAt(), base.getItems(),
				base.getTotals(), base.getXmlStorageRef(), InboundNfeStatus.PENDING_CONFERENCE, importedAt);
	}

	@Test
	void reportsEveryNumberOfAVoidedRangeAsAVoidedNumberAndSkipsOtherDocumentTypes() {
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of(
				range(FiscalDocumentType.NFE, "1", 101L, 103L, "2028-02-12T15:00:00Z"),
				range(FiscalDocumentType.NFCE, "2", 5L, 6L, "2028-02-12T15:00:00Z")));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(block('C').records()).extracting(GenerateSpedFiscalServiceTest::texts).containsExactly(
				java.util.Arrays.asList("1", "0", null, "55", "05", "1", "101", null),
				java.util.Arrays.asList("1", "0", null, "55", "05", "1", "102", null),
				java.util.Arrays.asList("1", "0", null, "55", "05", "1", "103", null));
	}

	@Test
	void sortsTheDocumentsByDayThenOperationThenSeriesAndNumber() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 30L,
						Instant.parse("2028-02-10T12:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 9L,
						Instant.parse("2028-02-10T13:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 2L,
						Instant.parse("2028-02-11T13:00:00Z"))));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				SpedFiscalFixtures.confirmed(LivrosFiscaisFixtures.receivedNfe(companyId, "1", "77", "Alfa SA",
						"1102", "1102", "10.00", "0", "0", "0", "0", Instant.parse("2028-02-10T08:00:00Z")))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(block('C').records()).extracting(record -> texts(record).get(0) + ":" + texts(record).get(6))
				.containsExactly("0:77", "1:9", "1:30", "1:2");
	}

	@Test
	void assessesTheIcmsOfTheRegularDocumentsDebitsAgainstCredits() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.CANCELLED, "5102", "1", 21L,
						Instant.parse("2028-02-11T12:00:00Z"))));
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				SpedFiscalFixtures.confirmed(LivrosFiscaisFixtures.receivedNfe(companyId, "1", "5", "Alfa SA",
						"1102", "1102", "300.00", "27.00", "0", "0", "0", Instant.parse("2028-02-05T08:00:00Z")))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		SpedBlock e = block('E');
		assertThat(e.records()).extracting(SpedRecord::register).containsExactly("E100", "E110");
		assertThat(texts(e.records().get(0))).containsExactly("01022028", "29022028");
		// debits 18,00 (the cancelled NFe counts for nothing); credits 27,00; a 9,00 credit to carry forward
		assertThat(texts(e.records().get(1))).containsExactly("18,00", "0,00", "0,00", "0,00", "27,00", "0,00",
				"0,00", "0,00", "0,00", "0,00", "0,00", "0,00", "9,00", "0,00");
	}

	@Test
	void declaresTheIcmsToPayWhenDebitsExceedCredits() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z"))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(texts(block('E').records().get(1))).containsExactly("18,00", "0,00", "0,00", "0,00", "0,00",
				"0,00", "0,00", "0,00", "0,00", "18,00", "0,00", "18,00", "0,00", "0,00");
	}

	@Test
	void listsOneParticipantPerCounterpartyWhateverTheNumberOfDocuments() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z")),
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 21L,
						Instant.parse("2028-02-11T12:00:00Z"))));

		service.execute(SpedFiscalFixtures.command(companyId, PERIOD));

		assertThat(block('0').records()).filteredOn(record -> record.register().equals("0150")).hasSize(1);
		assertThat(texts(block('0').records().get(2))).containsExactly("11222333000181", "Cliente SA", "01058",
				"11222333000181", null, "123456789", null, null, null, null, null, null);
	}

	@Test
	void saysWhatTheFileGoesOutWithoutInWarningsAndStillGeneratesIt() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 20L,
						Instant.parse("2028-02-10T12:00:00Z"))));

		SpedValidationReport report = service.execute(SpedFiscalFixtures.command(companyId, PERIOD)).report();

		assertThat(report.errors()).isEmpty();
		assertThat(report.warnings()).extracting(Issue::record).containsExactly("0150", "C170/C190", "E110");
	}

	@Test
	void failsListingEveryMissingOrInvalidRecordInsteadOfEmittingAnIncompleteFile() {
		Taxpayer taxpayer = new Taxpayer(" ", "355", null, null, null, "123", null, null);
		Accountant accountant = new Accountant(null, "111.111.111-11", "", null);
		GenerateSpedFiscalCommand command = new GenerateSpedFiscalCommand(companyId,
				new Period(LocalDate.of(2028, 2, 20), LocalDate.of(2028, 3, 5)), null, taxpayer, accountant);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOfSatisfying(SpedValidationException.class,
				ex -> {
					assertThat(ex.report().errors()).extracting(issue -> issue.record() + " " + issue.message())
							.containsExactlyInAnyOrder(
									"0000 DT_INI/DT_FIN: the EFD covers one calendar month, but the period spans "
											+ "2028-02-20 to 2028-03-05",
									"0000 NOME: is required",
									"0000 COD_MUN: the 7-digit IBGE municipality code is required",
									"0000 IND_PERFIL: the activity profile (A, B or C) is required",
									"0000 IND_ATIV: the activity type is required",
									"0005 CEP: the 8-digit zip code is required",
									"0100 NOME: is required", "0100 CRC: is required",
									"0100 CPF: the accountant's CPF is missing or invalid");
					assertThat(ex.getMessage()).contains("9 mandatory record(s)");
				});

		verifyNoInteractions(spedFile);
	}

	@Test
	void failsOnAReceivedNfeWhoseSeriesOrNumberCannotFillItsC100() {
		InboundNfe base = LivrosFiscaisFixtures.receivedNfe(companyId, "1", "10", "Alfa SA", "1102", "1102",
				"10.00", "0", "0", "0", "0", Instant.parse("2028-02-10T12:00:00Z"));
		InboundNfe broken = InboundNfe.of(base.getId(), companyId, base.getAccessKey(), "1234", "12A", base.getSupplierDocument(),
				"Alfa SA", base.getIssuedAt(), base.getItems(), base.getTotals(), "xml", InboundNfeStatus.CONFIRMED,
				base.getImportedAt(), SpedFiscalFixtures.confirmed(base).getConferenceResult());
		when(inbound.findConfirmedByCompanyBetween(any(), any(), any())).thenReturn(List.of(broken));

		assertThatThrownBy(() -> service.execute(SpedFiscalFixtures.command(companyId, PERIOD)))
				.isInstanceOfSatisfying(SpedValidationException.class, ex -> {
					assertThat(ex.report().errors()).allMatch(issue -> issue.record().equals("C100")
							&& issue.reference().equals("NFe recebida 1234/12A"));
					assertThat(ex.report().errors()).extracting(Issue::message).satisfiesExactlyInAnyOrder(
							message -> assertThat(message).startsWith("SER"),
							message -> assertThat(message).startsWith("NUM_DOC"));
				});

		verifyNoInteractions(spedFile);
	}

	@Test
	void failsWhenANumberIsBothVoidedAndIssued() {
		when(nfes.findAuthorizedOrCancelledByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				LivrosFiscaisFixtures.issuedNfe(companyId, NfeDocumentStatus.AUTHORIZED, "5102", "1", 102L,
						Instant.parse("2028-02-10T12:00:00Z"))));
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of(
				range(FiscalDocumentType.NFE, "1", 101L, 103L, "2028-02-12T15:00:00Z")));

		assertThatThrownBy(() -> service.execute(SpedFiscalFixtures.command(companyId, PERIOD)))
				.isInstanceOfSatisfying(SpedValidationException.class, ex -> {
					assertThat(ex.report().errors()).hasSize(1);
					assertThat(ex.report().errors().get(0).severity()).isEqualTo(Severity.ERROR);
					assertThat(ex.report().errors().get(0).message()).contains("more than once");
				});
	}

	@Test
	void failsOnAVoidedRangeBeyondTheNineDigitsOfTheNumber() {
		when(voided.findByCompanyIdAndVoidedAtBetween(any(), any(), any())).thenReturn(List.of(
				range(FiscalDocumentType.NFE, "1", 999_999_990L, 1_000_000_000L, "2028-02-12T15:00:00Z")));

		assertThatThrownBy(() -> service.execute(SpedFiscalFixtures.command(companyId, PERIOD)))
				.isInstanceOfSatisfying(SpedValidationException.class,
						ex -> assertThat(ex.report().errors().get(0).message()).contains("NUM_DOC"));
	}

	private VoidedNumberRange range(FiscalDocumentType type, String series, long start, long end, String at) {
		return VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), companyId, type, series, start, end,
				"formulários danificados", "protocol", Instant.parse(at));
	}

	private SpedLayout layout() {
		ArgumentCaptor<SpedLayout> captor = ArgumentCaptor.forClass(SpedLayout.class);
		verify(spedFile).generate(captor.capture());
		return captor.getValue();
	}

	private List<SpedBlock> blocks() {
		return layout().blocks();
	}

	private SpedBlock block(char letter) {
		return blocks().stream().filter(block -> block.id() == letter).findFirst().orElseThrow();
	}

	/** The fields as the port writes them: amounts with a comma, dates as ddMMyyyy, empty ones as null. */
	private static List<String> texts(SpedRecord record) {
		return record.fields().stream().map(field -> switch (field) {
			case null -> null;
			case java.math.BigDecimal amount -> amount.toPlainString().replace('.', ',');
			case LocalDate date -> java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy").format(date);
			default -> field.toString();
		}).toList();
	}
}
