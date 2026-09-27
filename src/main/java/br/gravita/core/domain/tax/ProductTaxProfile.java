package br.gravita.core.domain.tax;

import java.util.Objects;

public record ProductTaxProfile(String productRef, String ncm) {

	public ProductTaxProfile {
		Objects.requireNonNull(productRef, "productRef");
		Objects.requireNonNull(ncm, "ncm");
	}
}
