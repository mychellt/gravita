package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedLayout;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpedFileAdapterTest {

	private final SpedFileAdapter adapter = new SpedFileAdapter();

	@Test
	@DisplayName("Writes one register per line between pipes, ended by CRLF")
	void writesOneRegisterPerLineBetweenPipesEndedByCrLf() {
		String txt = write(layout(new SpedBlock('0', List.of(SpedRecord.of("0140", "1", null, "x")))));

		assertThat(txt).startsWith("|0000|006|0|\r\n|0001|0|\r\n|0140|1||x|\r\n|0990|4|\r\n");
		assertThat(txt).endsWith("\r\n").doesNotContain("\n|\n");
		assertThat(txt.split("\r\n")).allSatisfy(line -> assertThat(line).startsWith("|").endsWith("|"));
	}

	@Test
	@DisplayName("Writes amounts with a comma, dates as ddMMyyyy and text without characters that would split the register")
	void writesAmountsWithACommaDatesAsDdMmYyyyAndTextWithoutWhatWouldSplitTheRegister() {
		String txt = write(layout(new SpedBlock('0', List.of(SpedRecord.of("0150", new BigDecimal("1234.50"),
				new BigDecimal("1.6500"), LocalDate.of(2028, 2, 9), 7, 12L, " a|b\r\nc ")))));

		assertThat(txt).contains("|0150|1234,50|1,6500|09022028|7|12|a b  c|\r\n");
	}

	@Test
	@DisplayName("Rejects a field of a type it does not know how to write")
	void rejectsAFieldOfATypeItDoesNotKnowHowToWrite() {
		SpedLayout layout = layout(new SpedBlock('0', List.of(SpedRecord.of("0150", new Object()))));

		assertThatThrownBy(() -> adapter.generate(layout)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Encodes the file in ISO-8859-1 so each accent takes one byte")
	void encodesInIso88591SoAccentsAreOneByteEach() {
		byte[] bytes = adapter.generate(layout(new SpedBlock('0', List.of(SpedRecord.of("0150", "Ação")))));

		assertThat(new String(bytes, StandardCharsets.ISO_8859_1)).contains("|0150|Ação|");
		assertThat(new String(bytes, StandardCharsets.UTF_8)).doesNotContain("Ação");
	}

	@Test
	@DisplayName("Opens each block flagged with or without data and closes it counting its own lines")
	void opensEachBlockFlaggedWithOrWithoutDataAndClosesItCountingItsOwnLines() {
		String txt = write(layout(new SpedBlock('0', List.of(SpedRecord.of("0140", "1"))),
				new SpedBlock('A', List.of()),
				new SpedBlock('C', List.of(SpedRecord.of("C010", "1"), SpedRecord.of("C100", "x")))));

		assertThat(lines(txt)).containsSubsequence("|0000|006|0|", "|0001|0|", "|0140|1|", "|0990|4|", "|A001|1|",
				"|A990|2|", "|C001|0|", "|C010|1|", "|C100|x|", "|C990|4|");
	}

	@Test
	@DisplayName("Closes the file with a block 9 counting every register and every line")
	void closesTheFileWithABlockNineCountingEveryRegisterAndEveryLine() {
		String txt = write(layout(new SpedBlock('0', List.of(SpedRecord.of("0150", "a"), SpedRecord.of("0150", "b"))),
				new SpedBlock('A', List.of())));

		List<String> lines = lines(txt);
		// 0000 0001 0150 0150 0990 | A001 A990 | 9001 9900 x10 9990 9999
		assertThat(lines.subList(lines.indexOf("|9001|0|"), lines.size())).containsExactly("|9001|0|",
				"|9900|0000|1|", "|9900|0001|1|", "|9900|0150|2|", "|9900|0990|1|", "|9900|A001|1|", "|9900|A990|1|",
				"|9900|9001|1|", "|9900|9900|10|", "|9900|9990|1|", "|9900|9999|1|", "|9990|13|", "|9999|" + lines.size() + "|");
		assertThat(lines).hasSize(5 + 2 + 13);
		// Block 9 is 9001 + ten 9900 (six registers before it, four of its own) + 9990 + 9999.
		assertThat(lines.stream().filter(line -> line.startsWith("|9900|")).count()).isEqualTo(10);
		assertThat(lines.stream().filter(line -> line.startsWith("|99")).count() + 1).isEqualTo(13);
	}

	@Test
	@DisplayName("Rejects a layout whose first block is not block 0 or that supplies block 9")
	void rejectsALayoutWhoseFirstBlockIsNotBlockZeroOrThatSuppliesBlockNine() {
		SpedRecord header = SpedRecord.of("0000", "006");
		assertThatThrownBy(() -> new SpedLayout(header, List.of(new SpedBlock('C', List.of()))))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new SpedBlock('9', List.of())).isInstanceOf(IllegalArgumentException.class);
	}

	private static SpedLayout layout(SpedBlock... blocks) {
		return new SpedLayout(SpedRecord.of("0000", "006", "0"), List.of(blocks));
	}

	private String write(SpedLayout layout) {
		return new String(adapter.generate(layout), StandardCharsets.ISO_8859_1);
	}

	private static List<String> lines(String txt) {
		return List.of(txt.split("\r\n"));
	}
}
