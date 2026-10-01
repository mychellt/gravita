package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * The tax assessed over the authorized fiscal documents of a period, for the accountant to review ahead of SPED. A
 * read model projected from {@code tax}: each total is the sum of that tax over the NFe authorized and the NFSe
 * authorized in the period (ICMS, IPI, PIS and COFINS from NFe, ISS from NFSe). A tax no document stated totals zero.
 */
public record AssessedTaxSummary(YearMonth period, BigDecimal icms, BigDecimal ipi, BigDecimal pis,
		BigDecimal cofins, BigDecimal iss) {
}
