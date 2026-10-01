package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.FiscalBookEntry;
import br.gravita.core.ports.inbound.reporting.FiscalBookFlow;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.FiscalBooksQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.FiscalDocumentRecord;
import br.gravita.core.usercases.reporting.GetFiscalBooksService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GetFiscalBooksServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final LocalDate FROM = LocalDate.of(2028, 2, 1);
	private static final LocalDate TO = LocalDate.of(2028, 2, 29);
	private static final byte[] PDF = {1, 2, 3};

	private final TaxReadModelPort tax = mock(TaxReadModelPort.class);
	private final RenderPdfPort pdf = mock(RenderPdfPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private GetFiscalBooksService service;

	@BeforeEach
	void setUp() {
		service = new GetFiscalBooksService(tax, pdf, permissions);
		when(permissions.canView(user, "fiscal-books")).thenReturn(true);
		when(tax.entryDocuments(any(), any())).thenReturn(List.of());
		when(tax.exitDocuments(any(), any())).thenReturn(List.of());
		when(pdf.render(any())).thenReturn(PDF);
	}

	@Test
	void refusesAUserWhoseProfileCannotViewTheBooksWithoutReadingOrRenderingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "fiscal-books")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new FiscalBooksQuery(stranger, PERIOD)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(tax, pdf);
	}

	@Test
	void readsTheWholeRequestedMonthFromTax() {
		service.execute(new FiscalBooksQuery(user, PERIOD));

		verify(tax).entryDocuments(FROM, TO);
		verify(tax).exitDocuments(FROM, TO);
	}

	@Test
	void booksTheReceivedDocumentsAsEntriesInDocumentOrder() {
		when(tax.entryDocuments(any(), any())).thenReturn(List.of(
				document("NFE", 20, "1", "200", "Beta Ltda", "5102", "300.00", "36.00"),
				document("NFE", 5, "1", "90", "Alfa SA", "1102", "100.00", "12.00"),
				document("NFE", 20, "1", "150", "Gama ME", "1102", "50.00", "0")));

		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		assertThat(books.entries()).extracting(FiscalBookEntry::flow, FiscalBookEntry::date, FiscalBookEntry::number)
				.containsExactly(
						tuple(FiscalBookFlow.ENTRY, LocalDate.of(2028, 2, 5), "90"),
						tuple(FiscalBookFlow.ENTRY, LocalDate.of(2028, 2, 20), "150"),
						tuple(FiscalBookFlow.ENTRY, LocalDate.of(2028, 2, 20), "200"));
		assertThat(books.entries().getFirst().counterpartName()).isEqualTo("Alfa SA");
		assertThat(books.entries().getFirst().cfop()).isEqualTo("1102");
		assertThat(books.entries().getFirst().totalValue()).isEqualByComparingTo("100");
		assertThat(books.exits()).isEmpty();
	}

	@Test
	void booksTheAuthorizedNfeAndNfceAsExits() {
		when(tax.exitDocuments(any(), any())).thenReturn(List.of(
				document("NFCE", 10, "1", "7", null, null, "40.00", null),
				document("NFE", 10, "1", "3", "Cliente SA", "5102", "1000.00", "180.00")));

		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		assertThat(books.exits()).extracting(FiscalBookEntry::flow, FiscalBookEntry::documentModel)
				.containsExactly(tuple(FiscalBookFlow.EXIT, "NFCE"), tuple(FiscalBookFlow.EXIT, "NFE"));
		assertThat(books.exits().getFirst().icmsValue()).isEqualByComparingTo("0");
		assertThat(books.entries()).isEmpty();
	}

	@Test
	void assessesIcmsAsExitDebitsMinusEntryCredits() {
		when(tax.entryDocuments(any(), any())).thenReturn(List.of(
				document("NFE", 3, "1", "10", "Alfa SA", "1102", "500.00", "60.00"),
				document("NFE", 4, "1", "11", "Beta Ltda", "1102", "80.00", "0")));
		when(tax.exitDocuments(any(), any())).thenReturn(List.of(
				document("NFE", 8, "1", "20", "Cliente SA", "5102", "1000.00", "180.00"),
				document("NFCE", 9, "1", "21", null, null, "40.00", null)));

		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		assertThat(books.icmsDebit()).isEqualByComparingTo("180");
		assertThat(books.icmsCredit()).isEqualByComparingTo("60");
		assertThat(books.icmsBalance()).isEqualByComparingTo("120");
		// Only the documents that stated ICMS are assessed, the debits first.
		assertThat(books.icmsAssessment()).extracting(FiscalBookEntry::flow, FiscalBookEntry::number)
				.containsExactly(tuple(FiscalBookFlow.EXIT, "20"), tuple(FiscalBookFlow.ENTRY, "10"));
	}

	@Test
	void anAssessmentWithMoreCreditsThanDebitsIsANegativeBalanceToCarryForward() {
		when(tax.entryDocuments(any(), any()))
				.thenReturn(List.of(document("NFE", 3, "1", "10", "Alfa SA", "1102", "500.00", "60.00")));

		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		assertThat(books.icmsBalance()).isEqualByComparingTo("-60");
	}

	@Test
	void rendersThePdfFromTheSameThreeBooks() {
		when(tax.entryDocuments(any(), any()))
				.thenReturn(List.of(document("NFE", 3, "1", "10", "Alfa SA", "1102", "500.00", "60.00")));
		when(tax.exitDocuments(any(), any()))
				.thenReturn(List.of(document("NFE", 8, "1", "20", "Cliente SA", "5102", "1000.00", "180.00")));

		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		ArgumentCaptor<PdfReport> rendered = ArgumentCaptor.forClass(PdfReport.class);
		verify(pdf).render(rendered.capture());
		assertThat(books.pdf()).isEqualTo(PDF);
		assertThat(rendered.getValue().title()).isEqualTo("Livros Fiscais - 02/2028");
		assertThat(rendered.getValue().headerLines()).containsExactly("Período: 01/02/2028 a 29/02/2028");
		assertThat(rendered.getValue().sections()).extracting(section -> section.heading()).containsExactly(
				"Livro de Entradas", "Livro de Saídas", "Livro de Apuração do ICMS");
		assertThat(rendered.getValue().sections().get(0).rows()).singleElement().satisfies(row -> {
			assertThat(row).startsWith("03/02/2028", "NFE", "1", "10");
			assertThat(row).contains("Alfa SA", "1102", "500,00", "60,00");
		});
		assertThat(rendered.getValue().sections().get(2).footerLines()).containsExactly("Débitos (saídas): 180,00",
				"Créditos (entradas): 60,00", "Saldo (débitos - créditos): 120,00");
	}

	@Test
	void rendersTheTxtWithTheThreeBooksAndTheirTotals() {
		when(tax.entryDocuments(any(), any()))
				.thenReturn(List.of(document("NFE", 3, "1", "10", "Alfa SA", "1102", "1500.50", "60.00")));
		when(tax.exitDocuments(any(), any()))
				.thenReturn(List.of(document("NFE", 8, "1", "20", "Cliente SA", "5102", "1000.00", "180.00")));

		String txt = new String(service.execute(new FiscalBooksQuery(user, PERIOD)).txt(), StandardCharsets.UTF_8);

		assertThat(txt).startsWith("LIVROS FISCAIS - 02/2028\nPeríodo: 01/02/2028 a 29/02/2028\n");
		assertThat(txt).containsSubsequence("LIVRO DE ENTRADAS", "Alfa SA", "Documentos: 1 | Valor total: 1.500,50 | ICMS: 60,00",
				"LIVRO DE SAÍDAS", "Cliente SA", "Documentos: 1 | Valor total: 1.000,00 | ICMS: 180,00",
				"LIVRO DE APURAÇÃO DO ICMS", "Débito", "Crédito", "Saldo (débitos - créditos): 120,00");
	}

	@Test
	void answersEmptyBooksWithZeroTotalsForAPeriodWithoutDocuments() {
		FiscalBooks books = service.execute(new FiscalBooksQuery(user, PERIOD));

		assertThat(books.entries()).isEmpty();
		assertThat(books.exits()).isEmpty();
		assertThat(books.icmsAssessment()).isEmpty();
		assertThat(books.icmsBalance()).isEqualByComparingTo("0");
		assertThat(books.pdf()).isEqualTo(PDF);
		assertThat(new String(books.txt(), StandardCharsets.UTF_8)).contains("Documentos: 0 | Valor total: 0,00");
	}

	private FiscalDocumentRecord document(String model, int day, String series, String number, String counterpart,
			String cfop, String total, String icms) {
		return new FiscalDocumentRecord(model, LocalDate.of(2028, 2, day), series, number,
				"3528" + "0".repeat(40), counterpart, counterpart == null ? null : "12345678000190", cfop,
				new BigDecimal(total), icms == null ? null : new BigDecimal(icms));
	}
}
