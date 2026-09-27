package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@Getter
public final class SalesOrder {

	private final SalesOrderId id;
	private final QuoteId originQuoteId;
	private final UUID customerId;
	private final List<SalesOrderItem> items;
	private final SalesOrderStatus status;

	public static SalesOrder createFromQuote(SalesOrderId id, QuoteId originQuoteId, UUID customerId,
			List<SalesOrderItem> items) {
		Objects.requireNonNull(originQuoteId, "originQuoteId is required");
		Objects.requireNonNull(customerId, "customerId is required");
		return new SalesOrder(id, originQuoteId, customerId, requireNonEmptyItems(items), SalesOrderStatus.DRAFT);
	}

	public static SalesOrder of(SalesOrderId id, QuoteId originQuoteId, UUID customerId, List<SalesOrderItem> items,
			SalesOrderStatus status) {
		return new SalesOrder(id, originQuoteId, customerId, items, status);
	}

	public BigDecimal totalValue() {
		return items.stream().map(SalesOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static List<SalesOrderItem> requireNonEmptyItems(List<SalesOrderItem> items) {
		List<SalesOrderItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales order must have at least one item");
		}
		return copy;
	}
}
