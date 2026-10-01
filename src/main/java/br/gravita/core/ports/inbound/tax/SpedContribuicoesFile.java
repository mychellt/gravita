package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * The EFD Contribuições (PIS/COFINS) of a company's month: the TXT as the SPED validator reads it
 * ({@code ISO-8859-1}, CR LF) and the assessment its Block M carries, so the summary and the file cannot disagree.
 */
public record SpedContribuicoesFile(CompanyId companyId, YearMonth period, String fileName, byte[] txt,
		Assessment assessment) {

	/**
	 * How PIS/COFINS is levied on the company: {@code NON_CUMULATIVE} (Lucro Real) takes credits for what the
	 * company buys, {@code CUMULATIVE} (Lucro Presumido) takes none.
	 */
	public enum Incidence {
		NON_CUMULATIVE,
		CUMULATIVE
	}

	/**
	 * {@code exitDocuments} are the NFe the company issued under an exit CFOP; {@code entryDocuments} the NFe it
	 * received and confirmed, plus any it issued under an entry CFOP.
	 */
	public record Assessment(TaxRegime taxRegime, Incidence incidence, int exitDocuments, int entryDocuments,
			Contribution pis, Contribution cofins) {
	}

	/**
	 * One contribution over the period. {@code revenue} is the value of the goods sold that the contribution was
	 * levied on and {@code base} the calculation base stated on them; {@code contribution} is what was levied.
	 * {@code credit} is what the purchases earned (always zero under the cumulative regime), {@code creditUsed} the
	 * part of it set against {@code contribution} and {@code creditBalance} the rest, carried forward. {@code payable}
	 * is {@code contribution - creditUsed}.
	 */
	public record Contribution(BigDecimal revenue, BigDecimal base, BigDecimal contribution, BigDecimal credit,
			BigDecimal creditUsed, BigDecimal creditBalance, BigDecimal payable) {
	}
}
