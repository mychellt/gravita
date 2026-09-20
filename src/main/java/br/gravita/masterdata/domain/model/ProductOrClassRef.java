package br.gravita.masterdata.domain.model;

import br.gravita.shared.BusinessRuleException;
import java.util.Objects;

/**
 * Points a price table entry at either a specific product (UC-11) or a
 * product classification group, without this bounded context depending on
 * the `Product` aggregate directly. Existence is resolved by the caller
 * (product catalog or, later, `sales`) when the reference is actually used.
 */
public record ProductOrClassRef(ProductOrClassRefType type, String referenceId) {

	public ProductOrClassRef {
		Objects.requireNonNull(type, "Reference type is required");
		if (referenceId == null || referenceId.isBlank()) {
			throw new BusinessRuleException("Reference id is required");
		}
	}

	public static ProductOrClassRef product(String productId) {
		return new ProductOrClassRef(ProductOrClassRefType.PRODUCT, productId);
	}

	public static ProductOrClassRef productClass(String classId) {
		return new ProductOrClassRef(ProductOrClassRefType.PRODUCT_CLASS, classId);
	}
}
