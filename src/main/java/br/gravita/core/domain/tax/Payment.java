package br.gravita.core.domain.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;

public record Payment(PaymentMethodType method, BigDecimal amount) {

	public Payment {
		Objects.requireNonNull(method, "method is required");
		Objects.requireNonNull(amount, "amount is required");
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Payment amount must be positive: " + amount);
		}
	}
}
