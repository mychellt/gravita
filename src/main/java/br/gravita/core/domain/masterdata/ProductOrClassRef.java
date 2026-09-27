package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.Objects;

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
