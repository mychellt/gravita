package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

/** A tax withheld at source by the tomador on one RPS/NFSe. */
public record NfseWithholding(TaxType taxType, BigDecimal base, BigDecimal ratePercentage, BigDecimal amount) {

	public NfseWithholding {
		Objects.requireNonNull(taxType, "taxType");
		Objects.requireNonNull(base, "base");
		Objects.requireNonNull(ratePercentage, "ratePercentage");
		Objects.requireNonNull(amount, "amount");
	}
}
