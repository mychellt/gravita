package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Writes the SPED text layout (UC-M2-11, shared with UC-M2-12): {@code |REG|field|...|} per line, CRLF line ends,
 * ISO-8859-1. A block with no records is still written, opened with {@code IND_MOV = 1} and closed; block 9 counts
 * every register of the file, itself included.
 */
@Component
class SpedFileAdapter implements GenerateSpedFilePort {

	private static final String EOL = "\r\n";

	@Override
	public byte[] generate(List<SpedBlock> blocks) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		StringBuilder out = new StringBuilder();
		int lines = 0;
		for (SpedBlock block : blocks) {
			lines += write(out, counts, block);
		}
		lines += writeTrailer(out, counts, lines);
		return out.toString().getBytes(StandardCharsets.ISO_8859_1);
	}

	private static int write(StringBuilder out, Map<String, Integer> counts, SpedBlock block) {
		int written = 0;
		for (SpedRecord record : block.leading()) {
			line(out, counts, record);
			written++;
		}
		String movement = block.leading().isEmpty() && block.records().isEmpty() ? "1" : "0";
		line(out, counts, SpedRecord.of(block.letter() + "001", movement));
		for (SpedRecord record : block.records()) {
			line(out, counts, record);
		}
		written += 1 + block.records().size();
		// The closing record counts every line of the block, itself included.
		line(out, counts, SpedRecord.of(block.letter() + "990", String.valueOf(written + 1)));
		return written + 1;
	}

	/** Block 9: opening, one counter per register (the counters' own register included), closing, file total. */
	private static int writeTrailer(StringBuilder out, Map<String, Integer> counts, int linesBefore) {
		counts.merge("9001", 1, Integer::sum);
		counts.merge("9990", 1, Integer::sum);
		counts.merge("9999", 1, Integer::sum);
		counts.merge("9900", counts.size() + 1, Integer::sum);
		line(out, new LinkedHashMap<>(), SpedRecord.of("9001", "0"));
		for (Map.Entry<String, Integer> count : counts.entrySet()) {
			line(out, new LinkedHashMap<>(), SpedRecord.of("9900", count.getKey(), String.valueOf(count.getValue())));
		}
		int block9 = 2 + counts.size() + 1;
		line(out, new LinkedHashMap<>(), SpedRecord.of("9990", String.valueOf(block9)));
		line(out, new LinkedHashMap<>(), SpedRecord.of("9999", String.valueOf(linesBefore + block9)));
		return block9;
	}

	private static void line(StringBuilder out, Map<String, Integer> counts, SpedRecord record) {
		counts.merge(record.register(), 1, Integer::sum);
		out.append('|').append(record.register());
		for (String field : record.fields()) {
			out.append('|').append(clean(field));
		}
		out.append('|').append(EOL);
	}

	/** The delimiter and line breaks cannot appear inside a field. */
	private static String clean(String field) {
		return field == null ? "" : field.replaceAll("[|\\r\\n]+", " ").trim();
	}
}
