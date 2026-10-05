package br.gravita.core.usercases.tax;

import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import java.math.BigDecimal;

/**
 * The two contributions of the EFD Contribuições. Block M carries each in its own registers - the same shape under
 * different codes - so {@code creditRegister} ({@code M100}/{@code M500}), {@code creditBaseRegister}
 * ({@code M105}/{@code M505}), {@code totalRegister} ({@code M200}/{@code M600}) and {@code detailRegister}
 * ({@code M210}/{@code M610}) are all that tells them apart.
 */
enum SpedTax {

	PIS(TaxType.PIS, "1.65", "0.65", "M100", "M105", "M200", "M210"),
	COFINS(TaxType.COFINS, "7.60", "3.00", "M500", "M505", "M600", "M610");

	private final TaxType taxType;
	private final BigDecimal nonCumulativeRate;
	private final BigDecimal cumulativeRate;
	private final String creditRegister;
	private final String creditBaseRegister;
	private final String totalRegister;
	private final String detailRegister;

	SpedTax(final TaxType taxType, final String nonCumulativeRate, final String cumulativeRate, final String creditRegister,
			final String creditBaseRegister, final String totalRegister, final String detailRegister) {
		this.taxType = taxType;
		this.nonCumulativeRate = new BigDecimal(nonCumulativeRate);
		this.cumulativeRate = new BigDecimal(cumulativeRate);
		this.creditRegister = creditRegister;
		this.creditBaseRegister = creditBaseRegister;
		this.totalRegister = totalRegister;
		this.detailRegister = detailRegister;
	}

	TaxType taxType() {
		return taxType;
	}

	/** The statutory general rate, in percent: what a document is levied at unless a rule sets another. */
	BigDecimal basicRate(final Incidence incidence) {
		return incidence == Incidence.NON_CUMULATIVE ? nonCumulativeRate : cumulativeRate;
	}

	String creditRegister() {
		return creditRegister;
	}

	String creditBaseRegister() {
		return creditBaseRegister;
	}

	String totalRegister() {
		return totalRegister;
	}

	String detailRegister() {
		return detailRegister;
	}
}
