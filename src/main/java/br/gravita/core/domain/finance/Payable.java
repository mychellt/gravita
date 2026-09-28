package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class Payable {

	private final PayableId id;
	private final UUID supplierId;
	private final PayableOrigin origin;
	private final BigDecimal amount;
	private final LocalDate dueDate;
	private final List<CostCenterShare> costCenterSplit;
	private final PayableStatus status;
	private final UUID purchaseReceiptRef;
	private final Integer installmentNumber;
	private final Integer installments;
	private final LedgerScope scope;
	private final List<PayableAttachment> attachments;

	private Payable(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount, LocalDate dueDate,
			List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments, LedgerScope scope,
			List<PayableAttachment> attachments) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.supplierId = supplierId;
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.costCenterSplit = requireValidSplit(costCenterSplit);
		this.status = Objects.requireNonNull(status, "status is required");
		this.purchaseReceiptRef = purchaseReceiptRef;
		this.installmentNumber = installmentNumber;
		this.installments = installments;
		this.scope = scope == null ? LedgerScope.NONE : scope;
		this.attachments = attachments == null ? List.of() : List.copyOf(attachments);
	}

	/**
	 * A one-off expense not tied to a purchase receipt. {@code supplierId} is
	 * optional (pure expenses such as rent have none), and so is
	 * {@code costCenterSplit}; when given, its percentages must add up to 100.
	 */
	public static Payable createManual(PayableId id, UUID supplierId, BigDecimal amount, LocalDate dueDate,
			List<CostCenterShare> costCenterSplit) {
		return new Payable(id, supplierId, PayableOrigin.MANUAL, amount, dueDate, costCenterSplit,
				PayableStatus.OPEN, null, null, null, LedgerScope.NONE, null);
	}

	/**
	 * One installment of the payment terms of a confirmed purchase receipt.
	 * {@code installments} is the total number of installments of the receipt,
	 * {@code installmentNumber} (1-based) this title's position among them.
	 */
	public static Payable createFromPurchaseReceipt(PayableId id, UUID supplierId, UUID purchaseReceiptRef,
			BigDecimal amount, LocalDate dueDate, int installmentNumber, int installments) {
		Objects.requireNonNull(supplierId, "supplierId is required");
		Objects.requireNonNull(purchaseReceiptRef, "purchaseReceiptRef is required");
		if (installmentNumber < 1 || installmentNumber > installments) {
			throw new BusinessRuleException(
					"installmentNumber must be between 1 and " + installments + ": " + installmentNumber);
		}
		return new Payable(id, supplierId, PayableOrigin.PURCHASE_RECEIPT, amount, dueDate, null,
				PayableStatus.OPEN, purchaseReceiptRef, installmentNumber, installments, LedgerScope.NONE, null);
	}

	public static Payable of(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount,
			LocalDate dueDate, List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments) {
		return of(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments, LedgerScope.NONE);
	}

	public static Payable of(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount,
			LocalDate dueDate, List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments, LedgerScope scope) {
		return of(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments, scope, null);
	}

	public static Payable of(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount,
			LocalDate dueDate, List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments, LedgerScope scope,
			List<PayableAttachment> attachments) {
		return new Payable(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments, scope, attachments);
	}

	/** The same title placed in {@code scope} (company, branch and bank account). */
	public Payable inScope(LedgerScope scope) {
		return new Payable(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments, scope, attachments);
	}

	/** Still to be paid: {@code OPEN} or {@code APPROVED}. */
	public boolean isOutstanding() {
		return status == PayableStatus.OPEN || status == PayableStatus.APPROVED;
	}

	/**
	 * The part of {@code value} (this title's amount, or a payment against it)
	 * charged to {@code costCenterId}: all of it when {@code costCenterId} is
	 * {@code null}, its share by percentage when the title is split, and zero
	 * when the title is not charged to that cost center.
	 */
	public BigDecimal shareOf(BigDecimal value, UUID costCenterId) {
		if (costCenterId == null) {
			return value;
		}
		return costCenterSplit.stream().filter(share -> share.costCenterId().equals(costCenterId)).findFirst()
				.map(share -> value.multiply(share.percent()).divide(new BigDecimal("100"), 2,
						RoundingMode.HALF_UP))
				.orElse(BigDecimal.ZERO);
	}

	/**
	 * Replaces the cost center split (rateio). Unlike at creation, where the
	 * split is optional, the split given here must be non-empty; its percentages
	 * must add up to 100 with no repeated cost center. A cancelled payable no
	 * longer counts as an expense, so it cannot be split.
	 */
	public Payable withCostCenterSplit(List<CostCenterShare> split) {
		if (split == null || split.isEmpty()) {
			throw new BusinessRuleException("costCenterSplit is required");
		}
		if (status == PayableStatus.CANCELLED) {
			throw new BusinessRuleException("Payable " + id.value() + " cannot be split: " + status);
		}
		return new Payable(id, supplierId, origin, amount, dueDate, split, status, purchaseReceiptRef,
				installmentNumber, installments, scope, attachments);
	}

	/**
	 * The same title with {@code attachment} appended after the existing ones.
	 * Supporting documents may be added at any point of the title's life, a
	 * receipt after payment included.
	 */
	public Payable withAttachment(PayableAttachment attachment) {
		Objects.requireNonNull(attachment, "attachment is required");
		List<PayableAttachment> all = new ArrayList<>(attachments);
		all.add(attachment);
		return new Payable(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments, scope, all);
	}

	private static BigDecimal requirePositive(BigDecimal amount) {
		if (amount == null) {
			throw new BusinessRuleException("amount is required");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("amount must be positive: " + amount);
		}
		return amount;
	}

	private static List<CostCenterShare> requireValidSplit(List<CostCenterShare> split) {
		if (split == null || split.isEmpty()) {
			return List.of();
		}
		Set<UUID> costCenters = new HashSet<>();
		BigDecimal total = BigDecimal.ZERO;
		for (CostCenterShare share : split) {
			if (!costCenters.add(share.costCenterId())) {
				throw new BusinessRuleException("costCenterSplit repeats cost center " + share.costCenterId());
			}
			total = total.add(share.percent());
		}
		if (total.compareTo(new BigDecimal("100")) != 0) {
			throw new BusinessRuleException("costCenterSplit percentages must sum to 100: " + total);
		}
		return List.copyOf(split);
	}
}
