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

	private SalesReturn(SalesReturnId id, SalesOrderId orderId, List<SalesReturnItem> items, boolean total,
			FiscalDocumentRef returnNfeRef) {
		this.id = Objects.requireNonNull(id, "SalesReturnId is required");
		this.orderId = Objects.requireNonNull(orderId, "orderId is required");
		this.items = requireNonEmptyItems(items);
		this.total = total;
		this.returnNfeRef = returnNfeRef;
	}

	public static SalesReturn forOrder(SalesReturnId id, SalesOrder order, List<SalesReturnItem> items,
			List<SalesReturn> previousReturns) {
		if (order.getStatus() != SalesOrderStatus.INVOICED) {
			throw new BusinessRuleException("Only INVOICED orders can be returned, was " + order.getStatus());
		}
		List<SalesReturnItem> copy = requireNonEmptyItems(items);
		Map<UUID, BigDecimal> alreadyReturned = accumulate(previousReturns == null ? List.of() : previousReturns);
		validateAgainstOrder(order, copy, alreadyReturned);

		Map<UUID, BigDecimal> totalReturnedAfterThis = new HashMap<>(alreadyReturned);
		for (SalesReturnItem item : copy) {
			totalReturnedAfterThis.merge(item.productOrServiceId(), item.quantity(), BigDecimal::add);
		}
		boolean total = isTotalReturn(order, totalReturnedAfterThis);

		return new SalesReturn(id, order.getId(), copy, total, null);
	}

	public SalesReturn withNfeRef(FiscalDocumentRef returnNfeRef) {
		return new SalesReturn(id, orderId, items, total, returnNfeRef);
	}

	public static SalesReturn of(SalesReturnId id, SalesOrderId orderId, List<SalesReturnItem> items, boolean total,
			FiscalDocumentRef returnNfeRef) {
		return new SalesReturn(id, orderId, items, total, returnNfeRef);
	}

	private static void validateAgainstOrder(SalesOrder order, List<SalesReturnItem> items,
			Map<UUID, BigDecimal> alreadyReturned) {
		Map<UUID, BigDecimal> invoicedByProduct = new HashMap<>();
		for (SalesOrderItem orderItem : order.getItems()) {
			invoicedByProduct.put(orderItem.productOrServiceId(), orderItem.quantity());
		}

		for (SalesReturnItem item : items) {
			BigDecimal invoiced = invoicedByProduct.get(item.productOrServiceId());
			if (invoiced == null) {
				throw new BusinessRuleException(
						"Product or service " + item.productOrServiceId() + " was not part of the original order");
			}
			BigDecimal returnedSoFar = alreadyReturned.getOrDefault(item.productOrServiceId(), BigDecimal.ZERO);
			if (returnedSoFar.add(item.quantity()).compareTo(invoiced) > 0) {
				throw new BusinessRuleException("Return quantity for product or service " + item.productOrServiceId()
						+ " exceeds the quantity originally invoiced: " + invoiced);
			}
		}
	}

	private static boolean isTotalReturn(SalesOrder order, Map<UUID, BigDecimal> totalReturnedByProduct) {
		return order.getItems().stream()
				.allMatch(orderItem -> totalReturnedByProduct
						.getOrDefault(orderItem.productOrServiceId(), BigDecimal.ZERO)
						.compareTo(orderItem.quantity()) >= 0);
	}

	private static Map<UUID, BigDecimal> accumulate(List<SalesReturn> previousReturns) {
		Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
		for (SalesReturn previousReturn : previousReturns) {
			for (SalesReturnItem item : previousReturn.getItems()) {
				returnedByProduct.merge(item.productOrServiceId(), item.quantity(), BigDecimal::add);
			}
		}
		return returnedByProduct;
	}

	private static List<SalesReturnItem> requireNonEmptyItems(List<SalesReturnItem> items) {
		List<SalesReturnItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales return must have at least one item");
		}
		return copy;
	}
}
