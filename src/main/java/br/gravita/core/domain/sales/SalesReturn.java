package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Registers a full or partial return against an {@code INVOICED}
 * {@link SalesOrder} (UC-M7-07). Follows {@code purchasing.PurchaseReturn}'s
 * shape: previously-returned quantities accumulate across returns so that
 * this and every later return stay capped at what the order originally
 * invoiced, and {@code total} tracks whether every item has now been
 * returned in full.
 */
@Getter
public final class SalesReturn {

	private final SalesReturnId id;
	private final SalesOrderId orderId;
	private final List<SalesReturnItem> items;
	private final boolean total;
	private final FiscalDocumentRef returnNfeRef;

	public SalesReturn(final SalesReturnId id, final SalesOrderId orderId, final List<SalesReturnItem> items, final boolean total,
			final FiscalDocumentRef returnNfeRef) {
		this.id = Objects.requireNonNull(id, "SalesReturnId is required");
		this.orderId = Objects.requireNonNull(orderId, "orderId is required");
		this.items = requireNonEmptyItems(items);
		this.total = total;
		this.returnNfeRef = returnNfeRef;
	}

	public static SalesReturn forOrder(final SalesReturnId id, final SalesOrder order, final List<SalesReturnItem> items,
			final List<SalesReturn> previousReturns) {
		if (order.getStatus() != SalesOrderStatus.INVOICED) {
			throw new BusinessRuleException("Only INVOICED orders can be returned, was " + order.getStatus());
		}
		final List<SalesReturnItem> copy = requireNonEmptyItems(items);
		final Map<UUID, BigDecimal> alreadyReturned = accumulate(previousReturns == null ? List.of() : previousReturns);
		validateAgainstOrder(order, copy, alreadyReturned);

		final Map<UUID, BigDecimal> totalReturnedAfterThis = new HashMap<>(alreadyReturned);
		for (final SalesReturnItem item : copy) {
			totalReturnedAfterThis.merge(item.productOrServiceId(), item.quantity(), BigDecimal::add);
		}
		final boolean total = isTotalReturn(order, totalReturnedAfterThis);

		return new SalesReturn(id, order.getId(), copy, total, null);
	}

	public SalesReturn withNfeRef(final FiscalDocumentRef returnNfeRef) {
		return new SalesReturn(id, orderId, items, total, returnNfeRef);
	}

	public static SalesReturn of(final SalesReturnId id, final SalesOrderId orderId, final List<SalesReturnItem> items, final boolean total,
			final FiscalDocumentRef returnNfeRef) {
		return new SalesReturn(id, orderId, items, total, returnNfeRef);
	}

	private static void validateAgainstOrder(final SalesOrder order, final List<SalesReturnItem> items,
			final Map<UUID, BigDecimal> alreadyReturned) {
		final Map<UUID, BigDecimal> invoicedByProduct = new HashMap<>();
		for (final SalesOrderItem orderItem : order.getItems()) {
			invoicedByProduct.put(orderItem.productOrServiceId(), orderItem.quantity());
		}

		for (final SalesReturnItem item : items) {
			final BigDecimal invoiced = invoicedByProduct.get(item.productOrServiceId());
			if (invoiced == null) {
				throw new BusinessRuleException(
						"Product or service " + item.productOrServiceId() + " was not part of the original order");
			}
			final BigDecimal returnedSoFar = alreadyReturned.getOrDefault(item.productOrServiceId(), BigDecimal.ZERO);
			if (returnedSoFar.add(item.quantity()).compareTo(invoiced) > 0) {
				throw new BusinessRuleException("Return quantity for product or service " + item.productOrServiceId()
						+ " exceeds the quantity originally invoiced: " + invoiced);
			}
		}
	}

	private static boolean isTotalReturn(final SalesOrder order, final Map<UUID, BigDecimal> totalReturnedByProduct) {
		return order.getItems().stream()
				.allMatch(orderItem -> totalReturnedByProduct
						.getOrDefault(orderItem.productOrServiceId(), BigDecimal.ZERO)
						.compareTo(orderItem.quantity()) >= 0);
	}

	private static Map<UUID, BigDecimal> accumulate(final List<SalesReturn> previousReturns) {
		final Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
		for (final SalesReturn previousReturn : previousReturns) {
			for (final SalesReturnItem item : previousReturn.getItems()) {
				returnedByProduct.merge(item.productOrServiceId(), item.quantity(), BigDecimal::add);
			}
		}
		return returnedByProduct;
	}

	private static List<SalesReturnItem> requireNonEmptyItems(final List<SalesReturnItem> items) {
		final List<SalesReturnItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales return must have at least one item");
		}
		return copy;
	}
}
