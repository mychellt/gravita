package br.gravita.core.ports.outbound.tax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Lays records out as a SPED text file: the pipe-delimited, one-record-per-line layout the EFD ICMS/IPI (UC-M2-11)
 * and the EFD Contribuições (UC-M2-12) share. It is the SPED counterpart of {@link GenerateDanfePort}. The caller
 * decides which records exist and what they hold; the port writes each block's opening and closing records
 * ({@code X001}, {@code X990}) and block 9 - the counters of every register in the file - so a file cannot carry
 * a wrong line count.
 */
public interface GenerateSpedFilePort {

	/** {@code blocks} in file order, block 9 excluded: the port appends it. */
	byte[] generate(List<SpedBlock> blocks);

	/**
	 * {@code letter} is the block ({@code '0'}, {@code 'C'}, ...). {@code leading} is what the layout puts before the
	 * block's opening record (block 0's {@code 0000}); {@code records} follow it, the closing record excluded.
	 */
	record SpedBlock(char letter, List<SpedRecord> leading, List<SpedRecord> records) {

		public SpedBlock {
			leading = List.copyOf(leading);
			records = List.copyOf(records);
		}

		public SpedBlock(char letter, List<SpedRecord> records) {
			this(letter, List.of(), records);
		}
	}

	/** {@code fields} follow the register code in layout order; a {@code null} one is left empty. */
	record SpedRecord(String register, List<String> fields) {

		public SpedRecord {
			fields = Collections.unmodifiableList(new ArrayList<>(fields));
		}

		public static SpedRecord of(String register, String... fields) {
			return new SpedRecord(register, Arrays.asList(fields));
		}
	}
}
