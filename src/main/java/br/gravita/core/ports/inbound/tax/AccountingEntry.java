package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One fiscal document of the period, as one line of the accounting export. {@code date} is the day the document was
 * authorized (issued NFe) or issued by its supplier (received NFe). {@code counterpartName} and
 * {@code counterpartDocument} are the supplier of an {@code ENTRY} and the recipient of an {@code EXIT}.
 * {@code cfop} lists every distinct CFOP, separated by {@code /}, when a received NFe mixes several, and is
 * {@code null} when the document states none. A tax the document does not state is zero.
 */
public record AccountingEntry(Flow flow, LocalDate date, String series, String number, String accessKey,
		String counterpartName, String counterpartDocument, String cfop, BigDecimal totalValue, BigDecimal icmsValue,
		BigDecimal ipiValue, BigDecimal pisValue, BigDecimal cofinsValue) {

	/** Which way a document moved goods: {@code ENTRY} is a purchase received, {@code EXIT} a sale issued. */
	public enum Flow {
		ENTRY,
		EXIT
	}
}
