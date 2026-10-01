package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The three statutory books of a company's month and the tax summary that goes with them, before they are rendered.
 * The books are laid out from this and nothing else, so the PDF and the TXT cannot disagree with it.
 *
 * <p>The entry book lists the NFe the company received and the NFe it issued with an entry CFOP (1, 2 or 3); the
 * exit book lists the NFe it issued with an exit CFOP (5, 6 or 7), and carries the number ranges voided in the period
 * so a gap in its numbering is explained rather than left out. The ICMS assessment lists every booked document that
 * stated ICMS, the exits' as debits and the entries' as credits.
 */
public record LivrosFiscaisBooks(CompanyId companyId, String companyCnpj, String companyIe, YearMonth period,
		Book entryBook, Book exitBook, IcmsAssessment icmsAssessmentBook, TaxSummary taxSummary) {

	/** Which way a booked document moved goods: {@code ENTRY} is a purchase received, {@code EXIT} a sale issued. */
	public enum Flow {
		ENTRY,
		EXIT
	}

	/**
	 * One line of a book. {@code date} is the day the document was authorized (issued NFe) or issued by its supplier
	 * (received NFe). {@code counterpartName} and {@code counterpartDocument} are the supplier of an entry and the
	 * recipient of an exit. {@code cfop} lists every distinct CFOP, separated by {@code /}, when an entry mixes
	 * several, and is {@code null} when the document states none. A tax the document does not state is zero.
	 */
	public record Line(Flow flow, LocalDate date, String series, String number, String accessKey,
			String counterpartName, String counterpartDocument, String cfop, BigDecimal totalValue,
			BigDecimal icmsValue, BigDecimal ipiValue, BigDecimal pisValue, BigDecimal cofinsValue) {
	}

	/**
	 * A range of document numbers the company voided with SEFAZ ({@code startNumber..endNumber} inclusive, in
	 * {@code series}), so the numbers missing from the exit book are accounted for.
	 */
	public record VoidedRange(String series, long startNumber, long endNumber, String justification,
			String sefazProtocol, Instant voidedAt) {

		public long quantity() {
			return endNumber - startNumber + 1;
		}
	}

	/** {@code voidedRanges} is empty on the entry book: a company does not number the NFe it receives. */
	public record Book(List<Line> lines, List<VoidedRange> voidedRanges, BigDecimal totalValue) {
	}

	/**
	 * {@code balance} is {@code debit - credit}: positive is ICMS to pay, negative a credit to carry forward.
	 */
	public record IcmsAssessment(List<Line> lines, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
	}

	/**
	 * The four taxes of the period. For each, {@code onExits} is the tax stated on the exit book (owed),
	 * {@code onEntries} the tax stated on the entry book (creditable) and {@code balance} their difference.
	 */
	public record TaxSummary(Totals icms, Totals ipi, Totals pis, Totals cofins) {
	}

	public record Totals(BigDecimal onExits, BigDecimal onEntries, BigDecimal balance) {
	}
}
