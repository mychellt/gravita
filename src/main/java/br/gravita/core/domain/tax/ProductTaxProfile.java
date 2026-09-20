package br.gravita.core.domain.tax;

import java.util.Objects;

/**
 * The subset of a product's M1 tax profile the engine needs: the NCM code
 * that keys the rate table.
 */
public record ProductTaxProfile(String productRef, String ncm) {

	public ProductTaxProfile {
		Objects.requireNonNull(productRef, "productRef");
		Objects.requireNonNull(ncm, "ncm");
	}
}
