package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
	private final UUID approvedBy;
	private final UUID alcadaId;

	public static SalesOrder createFromQuote(SalesOrderId id, QuoteId originQuoteId, UUID customerId,
			List<SalesOrderItem> items) {
		Objects.requireNonNull(originQuoteId, "originQuoteId is required");
		Objects.requireNonNull(customerId, "customerId is required");
		return new SalesOrder(id, originQuoteId, customerId, requireNonEmptyItems(items), SalesOrderStatus.DRAFT,
				null, null);
	}

	public static SalesOrder of(SalesOrderId id, QuoteId originQuoteId, UUID customerId, List<SalesOrderItem> items,
			SalesOrderStatus status) {
		return of(id, originQuoteId, customerId, items, status, null, null);
	}

	public static SalesOrder of(SalesOrderId id, QuoteId originQuoteId, UUID customerId, List<SalesOrderItem> items,
			SalesOrderStatus status, UUID approvedBy, UUID alcadaId) {
		return new SalesOrder(id, originQuoteId, customerId, items, status, approvedBy, alcadaId);
	}

	public SalesOrder approve(UUID approvedBy, UUID alcadaId) {
		Objects.requireNonNull(approvedBy, "approvedBy is required");
		if (status != SalesOrderStatus.DRAFT) {
			throw new BusinessRuleException("Only DRAFT orders can be approved, was " + status);
		}
		return new SalesOrder(id, originQuoteId, customerId, items, SalesOrderStatus.APPROVED, approvedBy, alcadaId);
	}

	public BigDecimal totalValue() {
		return items.stream().map(SalesOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal totalDiscount() {
		return items.stream().map(SalesOrderItem::discount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal discountPercent() {
		BigDecimal subtotal = items.stream().map(SalesOrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (subtotal.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return totalDiscount().multiply(BigDecimal.valueOf(100)).divide(subtotal, 4, RoundingMode.HALF_UP);
	}

	private static List<SalesOrderItem> requireNonEmptyItems(List<SalesOrderItem> items) {
		List<SalesOrderItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales order must have at least one item");
		}
		return copy;
	}
}
