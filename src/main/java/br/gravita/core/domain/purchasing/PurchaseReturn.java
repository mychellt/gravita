package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class PurchaseReturn {

	private final PurchaseReturnId id;
	private final PurchaseReceiptId receiptId;
	private final List<PurchaseReturnItem> items;
	private final boolean total;
	private final String returnNfeRef;

	public PurchaseReturn(final PurchaseReturnId id, final PurchaseReceiptId receiptId, final List<PurchaseReturnItem> items,
			final boolean total, final String returnNfeRef) {
		this.id = Objects.requireNonNull(id, "PurchaseReturnId is required");
		this.receiptId = Objects.requireNonNull(receiptId, "receiptId is required");
		this.items = requireNonEmptyItems(items);
		this.total = total;
		this.returnNfeRef = returnNfeRef;
	}

	public static PurchaseReturn forReceipt(final PurchaseReturnId id, final PurchaseReceipt receipt,
			final List<PurchaseReturnItem> items, final List<PurchaseReturn> previousReturns) {
		if (receipt.getStatus() != PurchaseReceiptStatus.CONFIRMED) {
			throw new BusinessRuleException(
					"A return can only be made against a confirmed receipt, was " + receipt.getStatus());
		}
		final List<PurchaseReturnItem> copy = requireNonEmptyItems(items);
		final Map<UUID, BigDecimal> alreadyReturned = accumulate(previousReturns == null ? List.of() : previousReturns);
		validateAgainstReceipt(receipt, copy, alreadyReturned);

		final Map<UUID, BigDecimal> totalReturnedAfterThis = new HashMap<>(alreadyReturned);
		for (final PurchaseReturnItem item : copy) {
			totalReturnedAfterThis.merge(item.productId(), item.quantity(), BigDecimal::add);
		}
		final boolean total = isTotalReturn(receipt, totalReturnedAfterThis);

		return new PurchaseReturn(id, receipt.getId(), copy, total, null);
	}

	public PurchaseReturn withNfeRef(final String returnNfeRef) {
		return new PurchaseReturn(id, receiptId, items, total, returnNfeRef);
	}

	public static PurchaseReturn of(final PurchaseReturnId id, final PurchaseReceiptId receiptId, final List<PurchaseReturnItem> items,
			final boolean total, final String returnNfeRef) {
		return new PurchaseReturn(id, receiptId, items, total, returnNfeRef);
	}

	private static void validateAgainstReceipt(final PurchaseReceipt receipt, final List<PurchaseReturnItem> items,
			final Map<UUID, BigDecimal> alreadyReturned) {
		final Map<UUID, BigDecimal> receivedByProduct = new HashMap<>();
		for (final PurchaseReceiptItem receiptItem : receipt.getReceivedItems()) {
			receivedByProduct.put(receiptItem.productId(), receiptItem.receivedQty());
		}

		for (final PurchaseReturnItem item : items) {
			final BigDecimal received = receivedByProduct.get(item.productId());
			if (received == null) {
				throw new BusinessRuleException(
						"Product " + item.productId() + " was not part of the original receipt");
			}
			final BigDecimal returnedSoFar = alreadyReturned.getOrDefault(item.productId(), BigDecimal.ZERO);
			if (returnedSoFar.add(item.quantity()).compareTo(received) > 0) {
				throw new BusinessRuleException("Return quantity for product " + item.productId()
						+ " exceeds the quantity originally received: " + received);
			}
		}
	}

	private static boolean isTotalReturn(final PurchaseReceipt receipt, final Map<UUID, BigDecimal> totalReturnedByProduct) {
		return receipt.getReceivedItems().stream()
				.allMatch(receiptItem -> totalReturnedByProduct.getOrDefault(receiptItem.productId(), BigDecimal.ZERO)
						.compareTo(receiptItem.receivedQty()) >= 0);
	}

	private static Map<UUID, BigDecimal> accumulate(final List<PurchaseReturn> previousReturns) {
		final Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
		for (final PurchaseReturn previousReturn : previousReturns) {
			for (final PurchaseReturnItem item : previousReturn.getItems()) {
				returnedByProduct.merge(item.productId(), item.quantity(), BigDecimal::add);
			}
		}
		return returnedByProduct;
	}

	private static List<PurchaseReturnItem> requireNonEmptyItems(final List<PurchaseReturnItem> items) {
		final List<PurchaseReturnItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A purchase return must have at least one item");
		}
		return copy;
	}
}
