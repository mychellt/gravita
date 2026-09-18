package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

public record ProductVariantDomain(String color, String size, String barcode) {

	public ProductVariantDomain {
		if ((color == null || color.isBlank()) && (size == null || size.isBlank())) {
			throw new BusinessRuleException("A variant must define a color, a size, or both");
		}
	}
}
