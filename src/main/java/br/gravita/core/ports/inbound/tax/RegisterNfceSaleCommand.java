package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RegisterNfceSaleCommand(UUID sessionId, List<SaleItemCommand> items, BigDecimal totalDiscount,
		List<PaymentCommand> payments, String customerCpf, UUID priceTableId) {

	public RegisterNfceSaleCommand {
		Objects.requireNonNull(sessionId, "sessionId is required");
		if (items == null || items.isEmpty()) {
			throw new BusinessRuleException("At least one item is required");
		}
		if (payments == null || payments.isEmpty()) {
			throw new BusinessRuleException("At least one payment is required");
		}
	}

	public record SaleItemCommand(UUID productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal itemDiscount) {

		public SaleItemCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
			Objects.requireNonNull(unitPrice, "unitPrice is required");
		}
	}

	public record PaymentCommand(PaymentMethodType method, BigDecimal amount) {

		public PaymentCommand {
			Objects.requireNonNull(method, "method is required");
			Objects.requireNonNull(amount, "amount is required");
		}
	}
}
