package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One row of the parameterized service-tax table: the rate of one tax for a service code, optionally narrowed to a
 * municipality and/or the provider's tax regime ({@code null} = applies to any), plus when that tax is withheld at
 * source. Rows are data, so a new service/municipality/regime combination needs no code change.
 */
public record ServiceTaxRule(String serviceCode, String municipalityIbgeCode, TaxRegime regime, TaxType taxType,
		BigDecimal ratePercentage, WithholdingMode withholding) {

	public ServiceTaxRule {
		Objects.requireNonNull(serviceCode, "serviceCode");
		Objects.requireNonNull(taxType, "taxType");
		Objects.requireNonNull(ratePercentage, "ratePercentage");
		withholding = withholding == null ? WithholdingMode.NEVER : withholding;
	}

	/** Higher is more specific: a municipality match outweighs a regime match. */
	public int specificity() {
		return (municipalityIbgeCode != null ? 2 : 0) + (regime != null ? 1 : 0);
	}

	public boolean appliesTo(final String ibgeCode, final TaxRegime providerRegime) {
		return (municipalityIbgeCode == null || municipalityIbgeCode.equals(ibgeCode))
				&& (regime == null || regime == providerRegime);
	}

	public boolean isWithheldFor(final NfseTomador tomador) {
		return switch (withholding) {
			case NEVER -> false;
			case ALWAYS -> true;
			case TOMADOR_COMPANY -> tomador.isCompany();
		};
	}
}
