package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code priceTableId} is not part of the use-case ticket's literal field
 * list, but AC2's max-discount cap has to be checked against *some* price
 * table, and UC-02's {@code ProductSearchResult} does not (yet) carry one.
 * Callers that resolved a price table while building the cart pass its id
 * here; when omitted, discounts are accepted uncapped.
 */
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
