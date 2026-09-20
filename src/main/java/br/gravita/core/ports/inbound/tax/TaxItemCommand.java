package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.Objects;

public record TaxItemCommand(String productRef, BigDecimal quantity, BigDecimal unitPrice) {

	public TaxItemCommand {
		Objects.requireNonNull(productRef, "productRef");
		Objects.requireNonNull(quantity, "quantity");
		Objects.requireNonNull(unitPrice, "unitPrice");
	}
}
