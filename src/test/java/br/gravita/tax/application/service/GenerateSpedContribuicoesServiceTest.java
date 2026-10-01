package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.ports.inbound.tax.GenerateSpedContribuicoesCommand;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Contribution;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedLayout;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import br.gravita.core.usercases.tax.GenerateSpedContribuicoesService;
import br.gravita.tax.SpedContribuicoesFixtures;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerateSpedContribuicoesServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");
	private static final byte[] TXT = {4, 5};
	private static final String SUPPLIER = "11.222.333/0001-81";

	/** The fields each register has after its code, per the Guia Prático. */
	private static final Map<String, Integer> FIELDS = Map.ofEntries(Map.entry("0000", 13), Map.entry("0110", 4),
			Map.entry("0140", 8), Map.entry("0150", 11), Map.entry("0190", 2), Map.entry("0200", 11),
			Map.entry("C010", 2), Map.entry("C100", 28), Map.entry("C170", 36), Map.entry("M100", 14),
			Map.entry("M105", 9), Map.entry("M200", 12), Map.entry("M210", 12), Map.entry("M500", 14),
			Map.entry("M505", 9), Map.entry("M600", 12), Map.entry("M610", 12));

	private final CompanyRepositoryPort companies = mock(CompanyRepositoryPort.class);
	private final NfeRepositoryPort nfes = mock(NfeRepositoryPort.class);
	private final InboundNfeRepositoryPort inbound = mock(InboundNfeRepositoryPort.class);
	private final GenerateSpedFilePort sped = mock(GenerateSpedFilePort.class);

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final GenerateSpedContribuicoesService service = new GenerateSpedContribuicoesService(companies, nfes,
			inbound, sped, Clock.system(ZoneOffset.UTC));

	GenerateSpedContribuicoesServiceTest() {
		company(TaxRegime.LUCRO_REAL);
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.of());
		when(sped.generate(any())).thenReturn(TXT);
	}

	@Test
	void answersNotFoundForAnUnknownCompanyWithoutReadingOrWritingAnything() {
		CompanyId stranger = CompanyId.of(UUID.randomUUID());
		when(companies.findById(stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GenerateSpedContribuicoesCommand(stranger, PERIOD)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(nfes, inbound, sped);
	}

	@Test
	void refusesACompanyUnderTheSimplesNacionalWithoutReadingOrWritingAnything() {
		company(TaxRegime.SIMPLES_NACIONAL);

		assertThatThrownBy(() -> execute()).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Simples Nacional");

		verifyNoInteractions(nfes, inbound, sped);
	}

	@Test
	void readsTheCompanysDocumentsOverTheWholeMonthInTheClocksZone() {
		execute();

		verify(nfes).findAuthorizedByCompanyBetween(companyId, FROM, TO);
		verify(inbound).findIssuedByCompanyBetween(companyId, FROM, TO);

		new GenerateSpedContribuicoesService(companies, nfes, inbound, sped,
				Clock.system(ZoneId.of("America/Sao_Paulo")))
				.execute(new GenerateSpedContribuicoesCommand(companyId, PERIOD));
		verify(nfes).findAuthorizedByCompanyBetween(companyId, Instant.parse("2028-02-01T03:00:00Z"),
				Instant.parse("2028-03-01T03:00:00Z"));
	}

	@Test
	void returnsTheFileTheWriterProducedNamedAfterTheCompanyAndThePeriod() {
		SpedContribuicoesFile file = execute();

		assertThat(file.txt()).isSameAs(TXT);
		assertThat(file.companyId()).isEqualTo(companyId);
		assertThat(file.period()).isEqualTo(PERIOD);
		assertThat(file.fileName()).isEqualTo("EFD-Contribuicoes-11222333000181-202802.txt");
	}

	@Test
	void assessesTheNonCumulativeRegimeOfLucroRealWithCreditsOnPurchases() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		sale("6102", 13, "2028-02-11T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "100.00", "0.65", "3.00", "2028-02-12T10:00:00Z");

		SpedContribuicoesFile file = execute();

		assertThat(file.assessment().taxRegime()).isEqualTo(TaxRegime.LUCRO_REAL);
		assertThat(file.assessment().incidence()).isEqualTo(Incidence.NON_CUMULATIVE);
		assertThat(file.assessment().exitDocuments()).isEqualTo(2);
		assertThat(file.assessment().entryDocuments()).isEqualTo(1);
		// Credit is the purchase's 100.00 at the buyer's general rates, not the 0.65 / 3.00 the supplier stated.
		assertContribution(file.assessment().pis(), "200.00", "200.00", "3.30", "1.65", "1.65", "0.00", "1.65");
		assertContribution(file.assessment().cofins(), "200.00", "200.00", "15.20", "7.60", "7.60", "0.00", "7.60");

		List<SpedRecord> m = block(layout(), 'M');
		assertThat(registers(m)).containsExactly("M100", "M105", "M200", "M210", "M500", "M505", "M600", "M610");
		assertThat(fields(m, "M100")).containsExactly("101", "0", "100.00", "1.6500", "", "", "1.65", "0.00", "0.00",
				"0.00", "1.65", "0", "1.65", "0.00");
		assertThat(fields(m, "M105")).containsExactly("01", "50", "100.00", "0.00", "100.00", "100.00", "", "", "");
		assertThat(fields(m, "M200")).containsExactly("3.30", "1.65", "0.00", "1.65", "0.00", "0.00", "1.65", "0.00",
				"0.00", "0.00", "0.00", "1.65");
		assertThat(fields(m, "M210")).containsExactly("01", "200.00", "200.00", "1.6500", "", "", "3.30", "0.00",
				"0.00", "0.00", "0.00", "3.30");
		assertThat(fields(m, "M500")).containsExactly("101", "0", "100.00", "7.6000", "", "", "7.60", "0.00", "0.00",
				"0.00", "7.60", "0", "7.60", "0.00");
		assertThat(fields(m, "M600")).containsExactly("15.20", "7.60", "0.00", "7.60", "0.00", "0.00", "7.60", "0.00",
				"0.00", "0.00", "0.00", "7.60");
		assertThat(fields(m, "M610")).containsExactly("01", "200.00", "200.00", "7.6000", "", "", "15.20", "0.00",
				"0.00", "0.00", "0.00", "15.20");
		assertThat(fields(block(layout(), '0'), "0110")).containsExactly("1", "1", "1", "");
	}

	@Test
	void carriesForwardTheCreditThatExceedsTheContribution() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "600.00", "9.90", "45.60", "2028-02-12T10:00:00Z");

		SpedContribuicoesFile file = execute();

		assertContribution(file.assessment().pis(), "100.00", "100.00", "1.65", "9.90", "1.65", "8.25", "0.00");
		assertContribution(file.assessment().cofins(), "100.00", "100.00", "7.60", "45.60", "7.60", "38.00", "0.00");
		List<SpedRecord> m = block(layout(), 'M');
		// Used in part: IND_DESC_CRED 1, the rest is the balance of the credit.
		assertThat(fields(m, "M100")).containsExactly("101", "0", "600.00", "1.6500", "", "", "9.90", "0.00", "0.00",
				"0.00", "9.90", "1", "1.65", "8.25");
		assertThat(fields(m, "M200")).containsExactly("1.65", "1.65", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
				"0.00", "0.00", "0.00", "0.00");
	}

	@Test
	void assessesTheCumulativeRegimeOfLucroPresumidoWithoutCredits() {
		company(TaxRegime.LUCRO_PRESUMIDO);
		sale("5102", 12, "2028-02-10T10:00:00Z", "0.65", "3.00");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "600.00", "9.90", "45.60", "2028-02-12T10:00:00Z");

		SpedContribuicoesFile file = execute();

		assertThat(file.assessment().incidence()).isEqualTo(Incidence.CUMULATIVE);
		assertContribution(file.assessment().pis(), "100.00", "100.00", "0.65", "0.00", "0.00", "0.00", "0.65");
		assertContribution(file.assessment().cofins(), "100.00", "100.00", "3.00", "0.00", "0.00", "0.00", "3.00");

		List<SpedRecord> m = block(layout(), 'M');
		assertThat(registers(m)).containsExactly("M200", "M210", "M600", "M610");
		assertThat(fields(m, "M200")).containsExactly("0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.65",
				"0.00", "0.00", "0.65", "0.65");
		assertThat(fields(m, "M210").get(0)).isEqualTo("51");
		assertThat(fields(m, "M610")).containsExactly("51", "100.00", "100.00", "3.0000", "", "", "3.00", "0.00",
				"0.00", "0.00", "0.00", "3.00");
		assertThat(fields(block(layout(), '0'), "0110")).containsExactly("2", "", "", "9");
		// The purchase is still booked, as one that gives no credit.
		assertThat(itemFields(layout(), 1).get(23)).isEqualTo("70");
	}

	@Test
	void givesNoCreditToAPurchaseThatIsNotForResaleOrAsAnInputOrThatTheSupplierDidNotLevy() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1556", "100.00", "1.65", "7.60", "2028-02-11T10:00:00Z");
		purchase(InboundNfeStatus.CONFIRMED, "101", "1102", "100.00", "0.00", "0.00", "2028-02-12T10:00:00Z");
		purchase(InboundNfeStatus.CONFIRMED, "102", "1102", "100.00", "0.00", "7.60", "2028-02-13T10:00:00Z");

		SpedContribuicoesFile file = execute();

		// The third one bore COFINS but not PIS: a credit of the one, not of the other.
		assertContribution(file.assessment().pis(), "100.00", "100.00", "1.65", "0.00", "0.00", "0.00", "1.65");
		assertContribution(file.assessment().cofins(), "100.00", "100.00", "7.60", "7.60", "7.60", "0.00", "0.00");
		assertThat(registers(block(layout(), 'M'))).doesNotContain("M100", "M105");
		assertThat(itemFields(layout(), 1).get(23)).isEqualTo("70");
		assertThat(itemFields(layout(), 3).get(23)).isEqualTo("70");
		assertThat(itemFields(layout(), 3).get(29)).isEqualTo("50");
	}

	@Test
	void givesAnInputPurchaseItsOwnNatureOfCredit() {
		purchase(InboundNfeStatus.CONFIRMED, "100", "1101", "100.00", "1.65", "7.60", "2028-02-11T10:00:00Z");
		purchase(InboundNfeStatus.CONFIRMED, "101", "2403", "200.00", "3.30", "15.20", "2028-02-12T10:00:00Z");

		execute();

		List<SpedRecord> m = block(layout(), 'M');
		assertThat(m.stream().filter(record -> record.register().equals("M105")).map(this::stringFields).toList())
				.containsExactly(List.of("01", "50", "200.00", "0.00", "200.00", "200.00", "", "", ""),
						List.of("02", "50", "100.00", "0.00", "100.00", "100.00", "", "", ""));
		assertThat(fields(m, "M100").subList(2, 3)).containsExactly("300.00");
	}

	@Test
	void booksOnlyTheAuthorizedNfeAndTheConfirmedInboundNfe() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "100.00", "1.65", "7.60", "2028-02-11T10:00:00Z");
		purchase(InboundNfeStatus.PENDING_CONFERENCE, "101", "1102", "999.00", "1.65", "7.60",
				"2028-02-12T10:00:00Z");

		SpedContribuicoesFile file = execute();

		assertThat(file.assessment().entryDocuments()).isEqualTo(1);
		assertThat(block(layout(), 'C').stream().filter(record -> record.register().equals("C100")).toList())
				.hasSize(2);
		assertThat(block(layout(), 'C').toString()).doesNotContain("999");
	}

	@Test
	void assessesAnNfeTheCompanyIssuedUnderAnEntryCfopAsAPurchaseOfItsOwn() {
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.of(
				SpedContribuicoesFixtures.issuedNfe(companyId, "1102", "1", 7L, Instant.parse("2028-02-10T10:00:00Z"),
						"1.65", "7.60")));

		SpedContribuicoesFile file = execute();

		assertThat(file.assessment().exitDocuments()).isZero();
		assertThat(file.assessment().entryDocuments()).isEqualTo(1);
		assertContribution(file.assessment().pis(), "0.00", "0.00", "0.00", "1.65", "0.00", "1.65", "0.00");
		// Issued by the company itself (IND_EMIT 0), an entry (IND_OPER 0).
		assertThat(fields(block(layout(), 'C'), "C100").subList(0, 2)).containsExactly("0", "0");
	}

	@Test
	void leavesTheItemsOfASaleThatStatesNoContributionOutOfTheAssessment() {
		sale("5102", 12, "2028-02-10T10:00:00Z", null, null);
		sale("5102", 13, "2028-02-11T10:00:00Z", "1.65", "7.60");

		SpedContribuicoesFile file = execute();

		assertContribution(file.assessment().pis(), "100.00", "100.00", "1.65", "0.00", "0.00", "0.00", "1.65");
		List<String> untaxed = itemFields(layout(), 0);
		assertThat(untaxed.subList(23, 29)).containsExactly("49", "", "", "", "", "");
		assertThat(untaxed.subList(29, 35)).containsExactly("49", "", "", "", "", "");
		assertThat(block(layout(), 'M').stream().filter(record -> record.register().equals("M210"))).hasSize(1);
	}

	@Test
	void groupsSalesAtAnotherRateUnderTheDifferentiatedCodeAndDeclaresTheContributionType() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "0.65", "7.60");
		sale("5102", 13, "2028-02-11T10:00:00Z", "1.65", "7.60");

		SpedContribuicoesFile file = execute();

		assertContribution(file.assessment().pis(), "200.00", "200.00", "2.30", "0.00", "0.00", "0.00", "2.30");
		List<SpedRecord> m = block(layout(), 'M');
		assertThat(m.stream().filter(record -> record.register().equals("M210")).map(this::stringFields).toList())
				.containsExactly(
						List.of("01", "100.00", "100.00", "1.6500", "", "", "1.65", "0.00", "0.00", "0.00", "0.00",
								"1.65"),
						List.of("02", "100.00", "100.00", "0.6500", "", "", "0.65", "0.00", "0.00", "0.00", "0.00",
								"0.65"));
		assertThat(itemFields(layout(), 0).get(23)).isEqualTo("02");
		assertThat(fields(block(layout(), '0'), "0110")).containsExactly("1", "1", "2", "");
	}

	@Test
	void laysTheCompanyAndItsParticipantsItemsAndUnitsOutInBlockZero() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "100.00", "1.65", "7.60", "2028-02-11T10:00:00Z");

		SpedLayout layout = layout();

		assertThat(layout.header().register()).isEqualTo("0000");
		assertThat(stringFields(layout.header())).containsExactly("006", "0", "", "", "2028-02-01", "2028-02-29", "",
				"11222333000181", "SP", "", "", "00", "2");
		List<SpedRecord> zero = block(layout, '0');
		assertThat(registers(zero)).containsExactly("0110", "0140", "0150", "0150", "0190", "0200", "0200");
		assertThat(fields(zero, "0140")).containsExactly("11222333000181", "", "11222333000181", "SP", "123456789",
				"", "987654", "");
		assertThat(zero.stream().filter(record -> record.register().equals("0150")).map(this::stringFields).toList())
				.containsExactly(
						List.of("11222333000181", "Fornecedor Alfa", "01058", "11222333000181", "", "", "", "", "",
								"", ""),
						List.of("11444777000161", "Cliente SA", "01058", "11444777000161", "", "", "", "", "", "",
								""));
		assertThat(fields(zero, "0190")).containsExactly("UN", "UN");
		assertThat(zero.stream().filter(record -> record.register().equals("0200")).map(this::stringFields).toList())
				.containsExactly(
						List.of("00000000-0000-0000-0000-000000000042", "Arroz", "", "", "UN", "99", "", "", "", "",
								""),
						List.of("11222333000181-SKU-1", "Parafuso", "", "", "UN", "99", "73181500", "", "", "", ""));
	}

	@Test
	void laysTheDocumentsOutInBlockCByDateThenSeriesAndNumberWithAnItemRegisterEach() {
		sale("5102", 10, "2028-02-10T10:00:00Z", "1.65", "7.60");
		sale("5102", 9, "2028-02-10T08:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "5", "1102", "100.00", "1.65", "7.60", "2028-02-03T08:00:00Z");

		List<SpedRecord> c = block(layout(), 'C');

		assertThat(registers(c)).containsExactly("C010", "C100", "C170", "C100", "C170", "C100", "C170");
		assertThat(c.stream().filter(record -> record.register().equals("C100")).map(record -> stringFields(record)
				.get(6)).toList()).containsExactly("5", "9", "10");
		assertThat(fields(c, "C010")).containsExactly("11222333000181", "2");
	}

	@Test
	void writesTheDocumentAndItsItemsWithTheFieldsOfTheLayout() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");

		List<SpedRecord> c = block(layout(), 'C');

		assertThat(fields(c, "C100")).containsExactly("1", "0", "11444777000161", "55", "00", "1", "12",
				String.format("%044d", 12), "2028-02-10", "2028-02-10", "100.00", "9", "0.00", "0.00", "100.00", "9",
				"0.00", "0.00", "0.00", "", "", "", "", "", "1.65", "7.60", "", "");
		assertThat(fields(c, "C170")).containsExactly("1", "00000000-0000-0000-0000-000000000042", "Arroz", "10.00000",
				"UN", "100.00", "0.00", "0", "", "5102", "", "", "", "", "", "", "", "", "", "", "", "", "", "01",
				"100.00", "1.6500", "", "", "1.65", "01", "100.00", "7.6000", "", "", "7.60", "");
	}

	@Test
	void everyRegisterCarriesExactlyTheFieldsOfItsLayout() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");
		purchase(InboundNfeStatus.CONFIRMED, "100", "1102", "600.00", "9.90", "45.60", "2028-02-12T10:00:00Z");

		SpedLayout layout = layout();

		List<SpedRecord> all = new java.util.ArrayList<>(List.of(layout.header()));
		layout.blocks().forEach(block -> all.addAll(block.records()));
		assertThat(all).isNotEmpty().allSatisfy(record -> assertThat(record.fields())
				.as(record.register()).hasSize(FIELDS.get(record.register())));
		assertThat(all.stream().map(SpedRecord::register).distinct().toList())
				.containsExactlyInAnyOrderElementsOf(FIELDS.keySet());
	}

	@Test
	void writesEveryBlockInOrderLeavingEmptyTheOnesTheModuleHasNothingFor() {
		sale("5102", 12, "2028-02-10T10:00:00Z", "1.65", "7.60");

		SpedLayout layout = layout();

		assertThat(layout.blocks()).extracting(SpedBlock::id).containsExactly('0', 'A', 'C', 'D', 'F', 'I', 'M', 'P',
				'1');
		assertThat(layout.blocks()).filteredOn(block -> "ADFIP1".indexOf(block.id()) >= 0)
				.allSatisfy(block -> assertThat(block.records()).isEmpty());
	}

	@Test
	void writesAnEmptyPeriodAsAFileWithNoDocumentsOrAssessment() {
		SpedContribuicoesFile file = execute();

		assertThat(file.assessment().exitDocuments()).isZero();
		assertThat(file.assessment().entryDocuments()).isZero();
		assertContribution(file.assessment().pis(), "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00");
		assertContribution(file.assessment().cofins(), "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00");
		SpedLayout layout = layout();
		assertThat(layout.header().register()).isEqualTo("0000");
		assertThat(block(layout, 'C')).isEmpty();
		assertThat(block(layout, 'M')).isEmpty();
		assertThat(registers(block(layout, '0'))).containsExactly("0110", "0140");
	}

	@Test
	void classifiesTheCompanysActivityFromItsCnae() {
		company(TaxRegime.LUCRO_REAL, "2511000");
		execute();
		assertThat(stringFields(layout().header()).get(12)).isEqualTo("0");

		company(TaxRegime.LUCRO_REAL, "6201500");
		execute();
		assertThat(stringFields(lastLayout()).get(12)).isEqualTo("9");
	}

	// --- helpers ---

	private SpedContribuicoesFile execute() {
		return service.execute(new GenerateSpedContribuicoesCommand(companyId, PERIOD));
	}

	private void company(TaxRegime regime) {
		company(regime, "4712100");
	}

	private void company(TaxRegime regime, String cnae) {
		when(companies.findById(companyId)).thenReturn(Optional.of(Company.of(companyId, Document.cnpj(SUPPLIER),
				"123456789", "987654", cnae, regime, regime == TaxRegime.SIMPLES_NACIONAL,
				SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "11999999999", null, null)));
	}

	private final java.util.ArrayList<br.gravita.core.domain.tax.NfeDocument> issued = new java.util.ArrayList<>();
	private final java.util.ArrayList<br.gravita.core.domain.tax.InboundNfe> received = new java.util.ArrayList<>();

	private void sale(String cfop, long number, String authorizedAt, String pisRate, String cofinsRate) {
		issued.add(SpedContribuicoesFixtures.issuedNfe(companyId, cfop, "1", number, Instant.parse(authorizedAt),
				pisRate, cofinsRate));
		when(nfes.findAuthorizedByCompanyBetween(any(), any(), any())).thenReturn(List.copyOf(issued));
	}

	private void purchase(InboundNfeStatus status, String number, String cfop, String value, String pis,
			String cofins, String issuedAt) {
		received.add(SpedContribuicoesFixtures.receivedNfe(companyId, status, number, "11.222.333/0001-81",
				"Fornecedor Alfa", cfop, value, pis, cofins, Instant.parse(issuedAt)));
		when(inbound.findIssuedByCompanyBetween(any(), any(), any())).thenReturn(List.copyOf(received));
	}

	private SpedLayout layout() {
		execute();
		return lastLayout0();
	}

	private SpedLayout lastLayout0() {
		ArgumentCaptor<SpedLayout> captor = ArgumentCaptor.forClass(SpedLayout.class);
		org.mockito.Mockito.verify(sped, org.mockito.Mockito.atLeastOnce()).generate(captor.capture());
		return captor.getValue();
	}

	private SpedRecord lastLayout() {
		return lastLayout0().header();
	}

	private static List<SpedRecord> block(SpedLayout layout, char id) {
		return layout.blocks().stream().filter(block -> block.id() == id).findFirst().orElseThrow().records();
	}

	private static List<String> registers(List<SpedRecord> records) {
		return records.stream().map(SpedRecord::register).toList();
	}

	private List<String> fields(List<SpedRecord> records, String register) {
		return stringFields(records.stream().filter(record -> record.register().equals(register)).findFirst()
				.orElseThrow());
	}

	/** The fields of the {@code index}-th (from 0) C170, in the order of the layout. */
	private List<String> itemFields(SpedLayout layout, int index) {
		return stringFields(block(layout, 'C').stream().filter(record -> record.register().equals("C170"))
				.toList().get(index));
	}

	private List<String> stringFields(SpedRecord record) {
		return record.fields().stream().map(field -> switch (field) {
			case null -> "";
			case BigDecimal amount -> amount.toPlainString();
			default -> field.toString();
		}).collect(Collectors.toList());
	}

	private static void assertContribution(Contribution contribution, String revenue, String base, String levied,
			String credit, String used, String balance, String payable) {
		assertThat(contribution.revenue()).isEqualTo(new BigDecimal(revenue));
		assertThat(contribution.base()).isEqualTo(new BigDecimal(base));
		assertThat(contribution.contribution()).isEqualTo(new BigDecimal(levied));
		assertThat(contribution.credit()).isEqualTo(new BigDecimal(credit));
		assertThat(contribution.creditUsed()).isEqualTo(new BigDecimal(used));
		assertThat(contribution.creditBalance()).isEqualTo(new BigDecimal(balance));
		assertThat(contribution.payable()).isEqualTo(new BigDecimal(payable));
	}
}
