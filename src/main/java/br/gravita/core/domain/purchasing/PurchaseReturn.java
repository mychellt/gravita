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

	public PurchaseReturn(PurchaseReturnId id, PurchaseReceiptId receiptId, List<PurchaseReturnItem> items,
			boolean total, String returnNfeRef) {
		this.id = Objects.requireNonNull(id, "PurchaseReturnId is required");
		this.receiptId = Objects.requireNonNull(receiptId, "receiptId is required");
		this.items = requireNonEmptyItems(items);
		this.total = total;
		this.returnNfeRef = returnNfeRef;
	}

	public static PurchaseReturn forReceipt(PurchaseReturnId id, PurchaseReceipt receipt,
			List<PurchaseReturnItem> items, List<PurchaseReturn> previousReturns) {
		if (receipt.getStatus() != PurchaseReceiptStatus.CONFIRMED) {
			throw new BusinessRuleException(
					"A return can only be made against a confirmed receipt, was " + receipt.getStatus());
		}
		List<PurchaseReturnItem> copy = requireNonEmptyItems(items);
		Map<UUID, BigDecimal> alreadyReturned = accumulate(previousReturns == null ? List.of() : previousReturns);
		validateAgainstReceipt(receipt, copy, alreadyReturned);

		Map<UUID, BigDecimal> totalReturnedAfterThis = new HashMap<>(alreadyReturned);
		for (PurchaseReturnItem item : copy) {
			totalReturnedAfterThis.merge(item.productId(), item.quantity(), BigDecimal::add);
		}
		boolean total = isTotalReturn(receipt, totalReturnedAfterThis);

		return new PurchaseReturn(id, receipt.getId(), copy, total, null);
	}

	public PurchaseReturn withNfeRef(String returnNfeRef) {
		return new PurchaseReturn(id, receiptId, items, total, returnNfeRef);
	}

	public static PurchaseReturn of(PurchaseReturnId id, PurchaseReceiptId receiptId, List<PurchaseReturnItem> items,
			boolean total, String returnNfeRef) {
		return new PurchaseReturn(id, receiptId, items, total, returnNfeRef);
	}

	private static void validateAgainstReceipt(PurchaseReceipt receipt, List<PurchaseReturnItem> items,
			Map<UUID, BigDecimal> alreadyReturned) {
		Map<UUID, BigDecimal> receivedByProduct = new HashMap<>();
		for (PurchaseReceiptItem receiptItem : receipt.getReceivedItems()) {
			receivedByProduct.put(receiptItem.productId(), receiptItem.receivedQty());
		}

		for (PurchaseReturnItem item : items) {
			BigDecimal received = receivedByProduct.get(item.productId());
			if (received == null) {
				throw new BusinessRuleException(
						"Product " + item.productId() + " was not part of the original receipt");
			}
			BigDecimal returnedSoFar = alreadyReturned.getOrDefault(item.productId(), BigDecimal.ZERO);
			if (returnedSoFar.add(item.quantity()).compareTo(received) > 0) {
				throw new BusinessRuleException("Return quantity for product " + item.productId()
						+ " exceeds the quantity originally received: " + received);
			}
		}
	}

	private static boolean isTotalReturn(PurchaseReceipt receipt, Map<UUID, BigDecimal> totalReturnedByProduct) {
		return receipt.getReceivedItems().stream()
				.allMatch(receiptItem -> totalReturnedByProduct.getOrDefault(receiptItem.productId(), BigDecimal.ZERO)
						.compareTo(receiptItem.receivedQty()) >= 0);
	}

	private static Map<UUID, BigDecimal> accumulate(List<PurchaseReturn> previousReturns) {
		Map<UUID, BigDecimal> returnedByProduct = new HashMap<>();
		for (PurchaseReturn previousReturn : previousReturns) {
			for (PurchaseReturnItem item : previousReturn.getItems()) {
				returnedByProduct.merge(item.productId(), item.quantity(), BigDecimal::add);
			}
		}
		return returnedByProduct;
	}

	private static List<PurchaseReturnItem> requireNonEmptyItems(List<PurchaseReturnItem> items) {
		List<PurchaseReturnItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A purchase return must have at least one item");
		}
		return copy;
	}
}
