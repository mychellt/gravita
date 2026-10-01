package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TaxReadModelPort {

	/** The total of the orders invoiced over {@code [from, to]} (inclusive) that have an issued fiscal document. */
	BigDecimal invoicedTotal(LocalDate from, LocalDate to, UUID companyId);
}
