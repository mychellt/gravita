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
	private final List<UUID> stockReservationIds;
	private final String cancelReason;

	public static SalesOrder createFromQuote(SalesOrderId id, QuoteId originQuoteId, UUID customerId,
			List<SalesOrderItem> items) {
		Objects.requireNonNull(originQuoteId, "originQuoteId is required");
		Objects.requireNonNull(customerId, "customerId is required");
		return new SalesOrder(id, originQuoteId, customerId, requireNonEmptyItems(items), SalesOrderStatus.DRAFT,
				List.of(), null);
	}

	public static SalesOrder of(SalesOrderId id, QuoteId originQuoteId, UUID customerId, List<SalesOrderItem> items,
			SalesOrderStatus status, List<UUID> stockReservationIds, String cancelReason) {
		return new SalesOrder(id, originQuoteId, customerId, items, status,
				stockReservationIds == null ? List.of() : List.copyOf(stockReservationIds), cancelReason);
	}

	public BigDecimal totalValue() {
		return items.stream().map(SalesOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public boolean hasActiveStockReservation() {
		return status == SalesOrderStatus.APPROVED || status == SalesOrderStatus.IN_SEPARATION;
	}

	public SalesOrder cancel(String reason) {
		if (status == SalesOrderStatus.INVOICED) {
			throw new BusinessRuleException(
					"Invoiced orders cannot be cancelled through this operation; use the return flow instead");
		}
		if (status == SalesOrderStatus.CANCELLED) {
			throw new BusinessRuleException("Order " + id.value() + " is already cancelled");
		}
		return new SalesOrder(id, originQuoteId, customerId, items, SalesOrderStatus.CANCELLED, stockReservationIds,
				reason);
	}

	private static List<SalesOrderItem> requireNonEmptyItems(List<SalesOrderItem> items) {
		List<SalesOrderItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales order must have at least one item");
		}
		return copy;
	}
}
