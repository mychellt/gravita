package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class PurchaseReceipt {

	private final PurchaseReceiptId id;
	private final PurchaseOrderId orderId;
	private final List<PurchaseReceiptItem> receivedItems;
	private final List<InstallmentTerm> installmentTerms;
	private final PurchaseReceiptStatus status;
	private final boolean total;

	private PurchaseReceipt(PurchaseReceiptId id, PurchaseOrderId orderId, List<PurchaseReceiptItem> receivedItems,
			List<InstallmentTerm> installmentTerms, PurchaseReceiptStatus status) {
		this.id = Objects.requireNonNull(id, "PurchaseReceiptId is required");
		this.orderId = Objects.requireNonNull(orderId, "orderId is required");
		this.receivedItems = requireNonEmptyItems(receivedItems);
		this.installmentTerms = installmentTerms == null ? List.of() : List.copyOf(installmentTerms);
		this.status = Objects.requireNonNull(status, "status is required");
		this.total = this.receivedItems.stream()
				.allMatch(item -> item.receivedQty().compareTo(item.orderedQty()) >= 0);
	}

	public static PurchaseReceipt pending(PurchaseReceiptId id, PurchaseOrderId orderId,
			List<PurchaseReceiptItem> receivedItems) {
		return new PurchaseReceipt(id, orderId, receivedItems, List.of(), PurchaseReceiptStatus.PENDING_CONFERENCE);
	}

	public static PurchaseReceipt of(PurchaseReceiptId id, PurchaseOrderId orderId,
			List<PurchaseReceiptItem> receivedItems, List<InstallmentTerm> installmentTerms,
			PurchaseReceiptStatus status) {
		return new PurchaseReceipt(id, orderId, receivedItems, installmentTerms, status);
	}

	public PurchaseReceipt completeConference(List<InstallmentTerm> installmentTerms) {
		if (status != PurchaseReceiptStatus.PENDING_CONFERENCE) {
			throw new BusinessRuleException("Only a receipt pending conference can complete it, was " + status);
		}
		List<InstallmentTerm> terms = installmentTerms == null ? List.of() : List.copyOf(installmentTerms);
		if (terms.isEmpty()) {
			throw new BusinessRuleException("At least one installment term is required to complete conference");
		}
		return new PurchaseReceipt(id, orderId, receivedItems, terms, PurchaseReceiptStatus.CONFERENCE_COMPLETED);
	}

	public PurchaseReceipt confirm() {
		if (status == PurchaseReceiptStatus.CONFIRMED) {
			throw new BusinessRuleException("Purchase receipt already confirmed: " + id.value());
		}
		if (status != PurchaseReceiptStatus.CONFERENCE_COMPLETED) {
			throw new BusinessRuleException(
					"Only a receipt with completed physical conference can be confirmed, was " + status);
		}
		return new PurchaseReceipt(id, orderId, receivedItems, installmentTerms, PurchaseReceiptStatus.CONFIRMED);
	}

	private static List<PurchaseReceiptItem> requireNonEmptyItems(List<PurchaseReceiptItem> items) {
		List<PurchaseReceiptItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A purchase receipt must have at least one received item");
		}
		return copy;
	}
}
