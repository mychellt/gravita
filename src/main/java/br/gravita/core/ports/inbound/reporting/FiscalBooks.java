package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * The three statutory books of a period, structured and rendered. {@code entries} lists the purchases received and
 * {@code exits} the sales authorized; {@code icmsAssessment} lists every document that stated ICMS, entries as
 * credits and exits as debits. {@code icmsBalance} is {@code icmsDebit - icmsCredit}: positive is ICMS to pay,
 * negative a credit to carry forward. {@code pdf} and {@code txt} carry the same three books and the same totals.
 */
public record FiscalBooks(YearMonth period, List<FiscalBookEntry> entries, List<FiscalBookEntry> exits,
		List<FiscalBookEntry> icmsAssessment, BigDecimal icmsDebit, BigDecimal icmsCredit, BigDecimal icmsBalance,
		byte[] pdf, byte[] txt) {
}
