package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@Getter
public final class SalesOrder {

	private final SalesOrderId id;
	private final QuoteId originQuoteId;
	private final UUID customerId;
	private final UUID salespersonId;
	private final List<SalesOrderItem> items;
	private final SalesOrderStatus status;
	private final UUID approvedBy;
	private final UUID alcadaId;
	private final String cancelReason;
	private final LocalDate invoicedAt;

	public static SalesOrder createFromQuote(final SalesOrderId id, final QuoteId originQuoteId, final UUID customerId,
			final UUID salespersonId, final List<SalesOrderItem> items) {
		Objects.requireNonNull(originQuoteId, "originQuoteId is required");
		Objects.requireNonNull(customerId, "customerId is required");
		Objects.requireNonNull(salespersonId, "salespersonId is required");
		return new SalesOrder(id, originQuoteId, customerId, salespersonId, requireNonEmptyItems(items),
				SalesOrderStatus.DRAFT, null, null, null, null);
	}

	public static SalesOrder of(final SalesOrderId id, final QuoteId originQuoteId, final UUID customerId, final UUID salespersonId,
			final List<SalesOrderItem> items, final SalesOrderStatus status) {
		return of(id, originQuoteId, customerId, salespersonId, items, status, null, null, null, null);
	}

	public static SalesOrder of(final SalesOrderId id, final QuoteId originQuoteId, final UUID customerId, final UUID salespersonId,
			final List<SalesOrderItem> items, final SalesOrderStatus status, final UUID approvedBy, final UUID alcadaId) {
		return of(id, originQuoteId, customerId, salespersonId, items, status, approvedBy, alcadaId, null, null);
	}

	public static SalesOrder of(final SalesOrderId id, final QuoteId originQuoteId, final UUID customerId, final UUID salespersonId,
			final List<SalesOrderItem> items, final SalesOrderStatus status, final UUID approvedBy, final UUID alcadaId,
			final String cancelReason) {
		return of(id, originQuoteId, customerId, salespersonId, items, status, approvedBy, alcadaId, cancelReason,
				null);
	}

	public static SalesOrder of(final SalesOrderId id, final QuoteId originQuoteId, final UUID customerId, final UUID salespersonId,
			final List<SalesOrderItem> items, final SalesOrderStatus status, final UUID approvedBy, final UUID alcadaId, final String cancelReason,
			final LocalDate invoicedAt) {
		return new SalesOrder(id, originQuoteId, customerId, salespersonId, items, status, approvedBy, alcadaId,
				cancelReason, invoicedAt);
	}

	public SalesOrder approve(final UUID approvedBy, final UUID alcadaId) {
		Objects.requireNonNull(approvedBy, "approvedBy is required");
		if (status != SalesOrderStatus.DRAFT) {
			throw new BusinessRuleException("Only DRAFT orders can be approved, was " + status);
		}
		return new SalesOrder(id, originQuoteId, customerId, salespersonId, items, SalesOrderStatus.APPROVED,
				approvedBy, alcadaId, cancelReason, invoicedAt);
	}

	public SalesOrder invoice() {
		if (status != SalesOrderStatus.APPROVED && status != SalesOrderStatus.IN_SEPARATION) {
			throw new BusinessRuleException("Only APPROVED or IN_SEPARATION orders can be invoiced, was " + status);
		}
		return new SalesOrder(id, originQuoteId, customerId, salespersonId, items, SalesOrderStatus.INVOICED,
				approvedBy, alcadaId, cancelReason, LocalDate.now());
	}

	public boolean hasActiveStockReservation() {
		return status == SalesOrderStatus.APPROVED || status == SalesOrderStatus.IN_SEPARATION;
	}

	public SalesOrder cancel(final String reason) {
		if (status == SalesOrderStatus.INVOICED) {
			throw new BusinessRuleException(
					"Invoiced orders cannot be cancelled through this operation; use the return flow instead");
		}
		if (status == SalesOrderStatus.CANCELLED) {
			throw new BusinessRuleException("Order " + id.value() + " is already cancelled");
		}
		return new SalesOrder(id, originQuoteId, customerId, salespersonId, items, SalesOrderStatus.CANCELLED,
				approvedBy, alcadaId, reason, invoicedAt);
	}

	public BigDecimal totalValue() {
		return items.stream().map(SalesOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal totalDiscount() {
		return items.stream().map(SalesOrderItem::discount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal discountPercent() {
		final BigDecimal subtotal = items.stream().map(SalesOrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (subtotal.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return totalDiscount().multiply(BigDecimal.valueOf(100)).divide(subtotal, 4, RoundingMode.HALF_UP);
	}

	private static List<SalesOrderItem> requireNonEmptyItems(final List<SalesOrderItem> items) {
		final List<SalesOrderItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales order must have at least one item");
		}
		return copy;
	}
}
