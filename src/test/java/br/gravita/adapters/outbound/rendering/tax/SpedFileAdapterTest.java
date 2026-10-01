package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpedFileAdapterTest {

	private final SpedFileAdapter adapter = new SpedFileAdapter();

	@Test
	void writesEachBlockOpenedAndClosedAndBlockNineCountingEveryRegister() {
		byte[] file = adapter.generate(List.of(
				new SpedBlock('0', List.of(SpedRecord.of("0000", "a", "b")), List.of(SpedRecord.of("0150", "x"))),
				new SpedBlock('C', List.of(SpedRecord.of("C100", "1", "é|x\r\ny"))),
				new SpedBlock('D', List.of())));

		assertThat(new String(file, StandardCharsets.ISO_8859_1)).isEqualTo(String.join("\r\n",
				"|0000|a|b|", "|0001|0|", "|0150|x|", "|0990|4|",
				"|C001|0|", "|C100|1|é x y|", "|C990|3|",
				"|D001|1|", "|D990|2|",
				"|9001|0|", "|9900|0000|1|", "|9900|0001|1|", "|9900|0150|1|", "|9900|0990|1|", "|9900|C001|1|",
				"|9900|C100|1|", "|9900|C990|1|", "|9900|D001|1|", "|9900|D990|1|", "|9900|9001|1|",
				"|9900|9990|1|", "|9900|9999|1|", "|9900|9900|13|", "|9990|16|", "|9999|25|", ""));
	}

	@Test
	void countsRepeatedRegistersOnceInBlockNineWithTheirNumberOfLines() {
		byte[] file = adapter.generate(List.of(new SpedBlock('C',
				List.of(SpedRecord.of("C100", "1"), SpedRecord.of("C100", "2"), SpedRecord.of("C100", "3")))));

		List<String> lines = Arrays.asList(new String(file, StandardCharsets.ISO_8859_1).split("\r\n"));
		assertThat(lines).contains("|9900|C100|3|", "|C990|5|", "|9990|10|", "|9999|" + lines.size() + "|");
		assertThat(lines).hasSize(15);
	}

	@Test
	void leavesNullFieldsEmptyAndKeepsTheLatin1CharactersOfPortuguese() {
		byte[] file = adapter.generate(List.of(new SpedBlock('C',
				List.of(SpedRecord.of("C100", null, "Ação", null)))));

		assertThat(new String(file, StandardCharsets.ISO_8859_1)).contains("|C100||Ação||\r\n");
		assertThat(file).contains((byte) 0xE7, (byte) 0xE3);
	}
}
