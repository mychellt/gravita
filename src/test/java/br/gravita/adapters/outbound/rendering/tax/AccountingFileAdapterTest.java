package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.ports.inbound.tax.AccountingEntry;
import br.gravita.core.ports.inbound.tax.AccountingEntry.Flow;
import br.gravita.core.ports.inbound.tax.AccountingExportFormat;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountingFileAdapterTest {

	private final AccountingFileAdapter adapter = new AccountingFileAdapter();

	@Test
	@DisplayName("Writes the CSV with a header and one semicolon-separated row per entry")
	void writesTheCsvWithAHeaderAndOneSemicolonSeparatedRowPerEntry() {
		String csv = text(adapter.export(List.of(received(), issued()), AccountingExportFormat.CSV));

		assertThat(csv.split("\r\n")).containsExactly(
				"Data;Natureza;Série;Número;Chave de acesso;Participante;CNPJ/CPF;CFOP;Valor total;ICMS;IPI;PIS;COFINS",
				"05/02/2028;ENTRADA;2;9;" + KEY + ";Fornecedor Alfa;11222333000181;1102/1403;1234,50;27,00;5,00;1,00;4,00",
				"10/02/2028;SAIDA;1;20;" + KEY + ";Cliente SA;11222333000181;5102;115,00;18,00;0,00;1,65;7,60");
		assertThat(csv).endsWith("\r\n");
	}

	@Test
	@DisplayName("Writes only the header for an empty period")
	void writesOnlyTheHeaderForAnEmptyPeriod() {
		String csv = text(adapter.export(List.of(), AccountingExportFormat.CSV));

		assertThat(csv.split("\r\n")).hasSize(1);
		assertThat(csv).startsWith("Data;Natureza");
	}

	@Test
	@DisplayName("Quotes a CSV cell that holds a separator, a quote or a line break")
	void quotesACsvCellThatHoldsASeparatorAQuoteOrALineBreak() {
		AccountingEntry tricky = withName("Alfa; \"Beta\"\nLtda");

		String csv = text(adapter.export(List.of(tricky), AccountingExportFormat.CSV));

		assertThat(csv).contains(";\"Alfa; \"\"Beta\"\"\nLtda\";");
	}

	@Test
	@DisplayName("Keeps a CSV cell that looks like a formula from being evaluated as one")
	void keepsACsvCellThatLooksLikeAFormulaFromBeingOne() {
		for (String name : List.of("=HYPERLINK(\"http://x\")", "+1", "-1", "@SUM(A1)")) {
			String csv = text(adapter.export(List.of(withName(name)), AccountingExportFormat.CSV));

			assertThat(csv).as(name).doesNotContain(";" + name.charAt(0) + name.substring(1, 2))
					.contains("'" + name.charAt(0));
		}
	}

	@Test
	@DisplayName("Writes the TXT as fixed-width lines without a header")
	void writesTheTxtAsFixedWidthLinesWithoutAHeader() {
		String txt = text(adapter.export(List.of(received(), issued()), AccountingExportFormat.TXT));

		String[] lines = txt.split("\r\n");
		assertThat(lines).hasSize(2);
		assertThat(lines[0]).isEqualTo("05/02/2028 ENTRADA 2   9         " + KEY + " " + "Fornecedor Alfa".repeat(1)
				+ " ".repeat(40 - "Fornecedor Alfa".length()) + " 11222333000181 1102/1403      "
				+ String.format(" %15s %15s %15s %15s %15s", "1234,50", "27,00", "5,00", "1,00", "4,00"));
		assertThat(lines[1]).startsWith("10/02/2028 SAIDA   1   20        " + KEY + " Cliente SA");
		assertThat(lines[0].length()).isEqualTo(lines[1].length());
	}

	@Test
	@DisplayName("Cuts a long name to its field width in the TXT but not in the CSV")
	void cutsALongNameToItsFieldInTheTxtButNotInTheCsv() {
		String name = "Distribuidora de Materiais de ConstruçãoXYZ Ltda";
		AccountingEntry longName = withName(name);

		String txt = text(adapter.export(List.of(longName), AccountingExportFormat.TXT));
		String csv = text(adapter.export(List.of(longName), AccountingExportFormat.CSV));

		assertThat(txt).contains(" " + name.substring(0, 40) + " ").doesNotContain(name.substring(0, 41));
		assertThat(name.charAt(40)).isEqualTo('X');
		assertThat(csv).contains(";" + name + ";");
	}

	@Test
	@DisplayName("Prints absent values as empty and keeps an absent CFOP from shifting the TXT columns")
	void printsAbsentValuesAsEmptyAndKeepsAnAbsentCfopFromShiftingTheTxt() {
		AccountingEntry bare = new AccountingEntry(Flow.ENTRY, LocalDate.of(2028, 2, 5), null, null, null, null, null,
				null, new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

		assertThat(text(adapter.export(List.of(bare), AccountingExportFormat.CSV)).split("\r\n")[1])
				.isEqualTo("05/02/2028;ENTRADA;;;;;;;1,00;0,00;0,00;0,00;0,00");
		assertThat(text(adapter.export(List.of(bare), AccountingExportFormat.TXT)).split("\r\n")[0])
				.isEqualTo("05/02/2028 ENTRADA" + " ".repeat(1 + 3 + 1 + 9 + 1 + 44 + 1 + 40 + 1 + 14 + 1 + 15)
						+ String.format(" %15s %15s %15s %15s %15s", "1,00", "0,00", "0,00", "0,00", "0,00"));
	}

	@Test
	@DisplayName("Keeps the accents of names encoded as UTF-8")
	void keepsTheAccentsOfTheNamesAsUtf8() {
		byte[] csv = adapter.export(List.of(withName("Indústria Açúcar")), AccountingExportFormat.CSV);

		assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Indústria Açúcar").contains("Série");
	}

	private static final String KEY = "35000000000000000000000000000000000000000001";

	private static String text(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static AccountingEntry received() {
		return new AccountingEntry(Flow.ENTRY, LocalDate.of(2028, 2, 5), "2", "9", KEY, "Fornecedor Alfa",
				"11222333000181", "1102/1403", new BigDecimal("1234.5"), new BigDecimal("27"), new BigDecimal("5.00"),
				new BigDecimal("1"), new BigDecimal("4.000"));
	}

	private static AccountingEntry issued() {
		return new AccountingEntry(Flow.EXIT, LocalDate.of(2028, 2, 10), "1", "20", KEY, "Cliente SA",
				"11222333000181", "5102", new BigDecimal("115.00"), new BigDecimal("18.00"), BigDecimal.ZERO,
				new BigDecimal("1.65"), new BigDecimal("7.60"));
	}

	private static AccountingEntry withName(String name) {
		return new AccountingEntry(Flow.ENTRY, LocalDate.of(2028, 2, 5), "1", "1", KEY, name, "11222333000181",
				"1102", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}
}
