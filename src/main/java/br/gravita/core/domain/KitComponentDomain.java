package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;

import java.math.BigDecimal;
import java.util.UUID;

public record KitComponentDomain(UUID productId, BigDecimal quantity) {

	public KitComponentDomain {
		if (productId == null) {
			throw new BusinessRuleException("Kit component must reference a product");
		}
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Kit component quantity must be greater than zero");
		}
	}
}
