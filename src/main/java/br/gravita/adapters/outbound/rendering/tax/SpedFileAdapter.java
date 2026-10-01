package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Writes a SPED TXT (EFD ICMS/IPI and EFD Contribuições alike): {@code |REG|field|field|} per line, CR LF, in
 * ISO-8859-1. It frames each block the use case supplies with its {@code X001} (flagging whether it holds data) and
 * {@code X990} (counting the block's lines) and closes the file with Block 9, whose {@code 9900} counts every
 * register written, itself included.
 */
@Component
class SpedFileAdapter implements GenerateSpedFilePort {

	private static final String EOL = "\r\n";
	private static final char FIELD_SEPARATOR = '|';
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

	@Override
	public byte[] generate(SpedLayout layout) {
		List<SpedRecord> lines = new ArrayList<>();
		for (SpedBlock block : layout.blocks()) {
			int start = lines.size();
			boolean fileOpener = block.id() == '0';
			if (fileOpener) {
				lines.add(layout.header());
			}
			boolean hasData = fileOpener || !block.records().isEmpty();
			lines.add(SpedRecord.of(block.id() + "001", hasData ? "0" : "1"));
			lines.addAll(block.records());
			// The closer counts itself.
			lines.add(SpedRecord.of(block.id() + "990", lines.size() - start + 1));
		}
		lines.addAll(blockNine(lines));
		StringBuilder txt = new StringBuilder();
		lines.forEach(line -> txt.append(write(line)));
		return txt.toString().getBytes(StandardCharsets.ISO_8859_1);
	}

	/**
	 * Block 9: its opener, one {@code 9900} for every distinct register of the file (the four of Block 9 included, so
	 * the {@code 9900} of {@code 9900} counts them all), its closer and the file's total line count.
	 */
	private static List<SpedRecord> blockNine(List<SpedRecord> lines) {
		Map<String, Integer> perRegister = new LinkedHashMap<>();
		lines.forEach(line -> perRegister.merge(line.register(), 1, Integer::sum));
		int registers = perRegister.size() + 4;
		perRegister.put("9001", 1);
		perRegister.put("9900", registers);
		perRegister.put("9990", 1);
		perRegister.put("9999", 1);

		List<SpedRecord> block = new ArrayList<>();
		block.add(SpedRecord.of("9001", "0"));
		perRegister.forEach((register, count) -> block.add(SpedRecord.of("9900", register, count)));
		block.add(SpedRecord.of("9990", block.size() + 2));
		block.add(SpedRecord.of("9999", lines.size() + block.size() + 1));
		return block;
	}

	private static String write(SpedRecord line) {
		StringBuilder out = new StringBuilder().append(FIELD_SEPARATOR).append(line.register());
		for (Object field : line.fields()) {
			out.append(FIELD_SEPARATOR).append(format(field));
		}
		return out.append(FIELD_SEPARATOR).append(EOL).toString();
	}

	private static String format(Object field) {
		return switch (field) {
			case null -> "";
			case String text -> clean(text);
			case BigDecimal amount -> amount.toPlainString().replace('.', ',');
			case LocalDate date -> DATE.format(date);
			case Number number -> number.toString();
			default -> throw new IllegalArgumentException("Unsupported SPED field: " + field.getClass().getName());
		};
	}

	/** A pipe or a line break inside a value would split the register, so they are dropped. */
	private static String clean(String text) {
		return text.replace("|", " ").replace('\r', ' ').replace('\n', ' ').strip();
	}
}
