package br.gravita.core.ports.outbound.tax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Writes the TXT of a SPED file from the registers its use case laid out (UC-M2-11 SPED Fiscal and UC-M2-12 SPED
 * Contribuições). Both EFDs share the same physical shape - one register per line, fields between pipes, the file cut
 * into blocks that each open with {@code X001} and close with {@code X990}, and a Block 9 that counts every register
 * - so that shape lives here once, and each use case supplies only its own registers.
 *
 * <p>The caller supplies the registers of each block (never the {@code X001}/{@code X990} pair, nor Block 9) and the
 * {@code 0000} that opens the file. The adapter adds the openers, the closers and Block 9, so the counters they carry
 * cannot disagree with the registers actually written.
 */
public interface GenerateSpedFilePort {

	/** The file as bytes in the encoding the SPED validator reads (ISO-8859-1), lines ended by CR LF. */
	byte[] generate(SpedLayout layout);

	/**
	 * One register: {@code register} is its code ({@code C100}) and {@code fields} its values in layout order, after
	 * the code. A field is a {@link String}, a {@link java.math.BigDecimal} (written with a comma, at its own scale),
	 * a {@link java.time.LocalDate} (written {@code ddMMyyyy}), another {@link Number}, or {@code null} for a field
	 * left empty.
	 */
	record SpedRecord(String register, List<Object> fields) {

		public SpedRecord {
			Objects.requireNonNull(register, "register is required");
			// List.copyOf would reject the null that stands for an empty field.
			fields = fields == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(fields));
		}

		public static SpedRecord of(final String register, final Object... fields) {
			return new SpedRecord(register, Arrays.asList(fields));
		}
	}

	/** {@code id} is the block's letter or digit ({@code 0}, {@code A}, {@code C}, ... {@code 1}); never {@code 9}. */
	record SpedBlock(char id, List<SpedRecord> records) {

		public SpedBlock {
			if (id == '9') {
				throw new IllegalArgumentException("Block 9 is written by the port, not supplied");
			}
			records = records == null ? List.of() : List.copyOf(records);
		}
	}

	/**
	 * {@code header} is the {@code 0000} that opens the file, ahead of the {@code 0001} of block {@code 0}, which must
	 * be the first block; {@code blocks} are in file order. A block with no register of its own is still written,
	 * flagged as having no data.
	 */
	record SpedLayout(SpedRecord header, List<SpedBlock> blocks) {

		public SpedLayout {
			Objects.requireNonNull(header, "header is required");
			blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks are required"));
			if (blocks.isEmpty() || blocks.getFirst().id() != '0') {
				throw new IllegalArgumentException("Block 0 must come first");
			}
		}
	}
}
